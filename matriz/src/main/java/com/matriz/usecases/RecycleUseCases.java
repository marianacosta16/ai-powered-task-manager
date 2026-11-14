package com.matriz.usecases;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.ProjectStatus;
import com.matriz.recycle.DeletedProject;
import com.matriz.recycle.DeletedTask;
import com.matriz.recycle.RecycleBin;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.ForbiddenException;
import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.TaskCommands;
import com.matriz.tasks.TaskQueries;
import com.matriz.tasks.TaskRecord;
import com.matriz.tasks.TaskStatus;

/**
 * deleteProject, deleteTask, listRecycleBin, restoreProject and restoreTask (F09; US07; R9, R20). Deleting
 * sends the item to the recycle bin for 30 days with everything deleted with it, all with the same instant;
 * restoring brings back only what has that instant, so what was deleted before stays in the bin (R9). The
 * rows are locked in the order of {@link Locks}: people first, then tasks, by id.
 */
@Service
public class RecycleUseCases {

    private static final String NOT_FOUND = "The item was not found in the recycle bin.";

    /** Why an item in the bin cannot come back now: the reason of the contract, the error code and the text. */
    private enum NotRestorable {
        PROJECT_IN_RECYCLE_BIN("CANNOT_RESTORE", "The project of this task is in the recycle bin. Restore the project first."),
        PARENT_IN_RECYCLE_BIN("CANNOT_RESTORE", "The parent task is in the recycle bin. Restore the parent task first."),
        PROJECT_COMPLETED("PROJECT_COMPLETED", "The project is completed, so nothing in it can be restored.");

        private final String code;
        private final String message;

        NotRestorable(String code, String message) {
            this.code = code;
            this.message = message;
        }

        BusinessRuleException toException() {
            return new BusinessRuleException(code, message);
        }
    }

    private final TaskRules rules;
    private final TaskQueries tasks;
    private final TaskCommands taskCommands;
    private final ProjectQueries projects;
    private final ProjectPermissions permissions;
    private final UserDirectory users;
    private final RecycleBin bin;
    private final Locks locks;
    private final Clock clock;

    RecycleUseCases(TaskRules rules, TaskQueries tasks, TaskCommands taskCommands, ProjectQueries projects,
            ProjectPermissions permissions, UserDirectory users, RecycleBin bin, Locks locks, Clock clock) {
        this.rules = rules;
        this.tasks = tasks;
        this.taskCommands = taskCommands;
        this.projects = projects;
        this.permissions = permissions;
        this.users = users;
        this.bin = bin;
        this.locks = locks;
        this.clock = clock;
    }

    // ---- deleting ----

    /**
     * Any member of the project and its manager delete a task or a subtask (R9), in an open or suspended
     * project (R27, R28). The task goes with its subtasks, which are all closed: one with an open subtask is refused.
     */
    @Transactional
    public void deleteTask(AuthenticatedUser user, long taskId) {
        TaskContext context = rules.loadForChange(user, taskId); // 404, 403 and R28
        if (context.task().isSubtask()) {
            locks.task(context.task().parentTaskId()); // the parent first, then the task: the order of updateTask
        }
        TaskRecord task = locks.task(taskId).orElseThrow(() -> new NotFoundException("The task was not found."));
        if (!task.isSubtask() && tasks.hasOpenSubtasks(taskId)) {
            throw new BusinessRuleException("PARENT_HAS_OPEN_SUBTASKS",
                    "This task has subtasks that are still open. Close or delete them first.");
        }
        bin.deleteTask(taskId, clock.instant());
    }

    /** Only the manager deletes a project, and not a completed one (R9, R28). */
    @Transactional
    public void deleteProject(AuthenticatedUser user, long projectId) {
        rules.loadProject(user, projectId); // 404 and 403
        permissions.requireManager(user, projectId);
        ProjectRecord project = projects.findByIdForUpdate(projectId)
                .orElseThrow(() -> new NotFoundException("The project was not found."));
        if (ProjectStatus.fromLabel("status", project.status()) == ProjectStatus.COMPLETED) {
            throw new BusinessRuleException("PROJECT_COMPLETED", "A completed project cannot be deleted.");
        }
        locks.tasksOfProject(projectId);
        bin.deleteProject(projectId, clock.instant());
    }

    // ---- reading ----

    /**
     * What the user can see in the bin (R8, R9): the projects they manage, and the tasks and subtasks of the
     * projects they manage or belong to, without what was deleted together with its project or its parent.
     * Newest first. Items past the 30 days are not there (R20).
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<RecycleItemView> listRecycleBin(AuthenticatedUser user) {
        Instant now = clock.instant();
        List<Entry> entries = new ArrayList<>();
        for (DeletedProject p : bin.projectsOfManager(user.id(), now)) {
            entries.add(new Entry(p.deletedAt(), p.id(), new RecycleItemView("Project", p.id(), p.name(), null, p.deletedAt(),
                    daysLeft(p.deletedAt(), now), p.includedTasks(), true, null)));
        }
        for (DeletedTask t : bin.tasksVisibleTo(user.id(), now)) {
            Optional<NotRestorable> reason = whyNot(t);
            entries.add(new Entry(t.deletedAt(), t.id(), new RecycleItemView(t.isSubtask() ? "Subtask" : "Task", t.id(), t.title(),
                    t.projectName(), t.deletedAt(), daysLeft(t.deletedAt(), now), null, reason.isEmpty(),
                    reason.map(Enum::name).orElse(null))));
        }
        return entries.stream().sorted(Comparator.comparing(Entry::deletedAt).reversed().thenComparing(Entry::id))
                .map(Entry::view).toList();
    }

    // ---- restoring ----

    /** Only the manager restores a project, as it was when it was deleted (R9). Members of other open projects are only warned (A37). */
    @Transactional
    public void restoreProject(AuthenticatedUser user, long projectId) {
        Instant now = clock.instant();
        DeletedProject project = bin.findProject(projectId, now).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (project.managerId() != user.id()) {
            throw new ForbiddenException("Only the manager of the project can restore it.");
        }
        DeletedProject locked = bin.lockProject(projectId, now).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        bin.restoreProject(projectId, locked.deletedAt());
    }

    /**
     * A member of the project or its manager restores a task or a subtask (R9), with the subtasks deleted with
     * it. The body, if any, gives the people who take over from those who left the project (P36, A42); it is
     * read after 404 and 403 are checked.
     *
     * <p>The state of the project and of the parent is checked after they are locked, in the order project,
     * people, parent, task: a parent that is closed, or a project that is deleted or completed, while the
     * restore is under way, is seen by it (R4, P35, R28).
     */
    @Transactional
    public void restoreTask(AuthenticatedUser user, long taskId, Supplier<List<Long>> body) {
        Instant now = clock.instant();
        DeletedTask item = bin.findTask(taskId, now).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (item.managerId() != user.id() && !bin.isMember(item.projectId(), user.id())) {
            throw new ForbiddenException("You have no access to this project.");
        }
        List<Long> newAssignees = body.get();
        if (newAssignees != null) {
            requireValidShape(newAssignees);
        }
        ProjectRecord project = projects.findByIdForUpdate(item.projectId())
                .orElseThrow(NotRestorable.PROJECT_IN_RECYCLE_BIN::toException); // deleted, or being deleted
        if (newAssignees != null) {
            locks.people(newAssignees); // the people before the tasks (see Locks)
            newAssignees.forEach(id -> rules.requireEligible(project, id));
        }
        TaskRecord parent = null;
        if (item.isSubtask()) {
            parent = locks.task(item.parentId()).orElseThrow(NotRestorable.PARENT_IN_RECYCLE_BIN::toException);
        }
        if (ProjectStatus.fromLabel("status", project.status()) == ProjectStatus.COMPLETED) {
            throw NotRestorable.PROJECT_COMPLETED.toException();
        }
        // The task is read again now that the project and the parent are locked: the first read, which decided 404
        // and 403, took no lock, so it may be out of date (it may have been eliminated or restored meanwhile).
        DeletedTask locked = locks.deletedTask(taskId, now).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        if (parent != null) {
            requireParentAllowsSubtask(locked, parent);
        }
        List<Long> restored = bin.idsDeletedWith(taskId, locked.deletedAt());
        replaceWhoLeft(project, restored, newAssignees);
        bin.restoreTask(taskId, locked.deletedAt());
    }

    /** P35 and R2 again: the parent changed while the subtask was in the bin. */
    private static void requireParentAllowsSubtask(DeletedTask subtask, TaskRecord parent) {
        if (parent.statusValue().isFinal() && !TaskStatus.ofStored(subtask.status()).isFinal()) {
            throw new BusinessRuleException("PARENT_TASK_CLOSED",
                    "The parent task is closed. Reopen it before restoring this open subtask.");
        }
        if (subtask.deadline().isAfter(parent.deadline())) {
            throw new BusinessRuleException("SUBTASK_DEADLINE_AFTER_PARENT",
                    "The deadline of this subtask is later than the one of its parent task. Change the parent deadline first.");
        }
    }

    /** At least one person, each once; the people are checked later, after they are locked. */
    private static void requireValidShape(List<Long> ids) {
        if (ids.isEmpty()) {
            throw new InvalidRequestException("The list of assignees is empty.", "assigneeIds", "At least one person.");
        }
        if (ids.size() != new HashSet<>(ids).size()) {
            throw new InvalidRequestException("An assignee is repeated.", "assigneeIds", "Each person once.");
        }
    }

    /**
     * P36: in each task that comes back, those who are no longer in the project (nor its manager) are replaced
     * by the people given. Without them the restore is refused and says who left; the others keep their tasks.
     */
    private void replaceWhoLeft(ProjectRecord project, List<Long> taskIds, List<Long> newAssignees) {
        Map<Long, List<Long>> current = tasks.assigneeIdsByTask(taskIds);
        Map<Long, List<Long>> departedByTask = new HashMap<>();
        for (Long id : taskIds) {
            List<Long> departed = current.getOrDefault(id, List.of()).stream()
                    .filter(userId -> !rules.isEligible(project, userId)).toList();
            if (!departed.isEmpty()) {
                departedByTask.put(id, departed);
            }
        }
        if (departedByTask.isEmpty()) {
            if (newAssignees != null) { // nobody to replace: refused, so the client does not think they were added
                throw new InvalidRequestException("Nobody has left the project, so nobody is replaced.", "assigneeIds",
                        "Restore without assigneeIds: nobody who was assigned has left the project.");
            }
            return;
        }
        if (newAssignees == null) {
            List<Long> left = departedByTask.values().stream().flatMap(List::stream).distinct().sorted().toList();
            Map<Long, UserInfo> people = users.findByIds(left);
            String names = String.join(", ", left.stream().map(id -> people.get(id).name()).toList());
            throw new BusinessRuleException("ASSIGNEE_REQUIRED", names + (left.size() == 1 ? " is" : " are")
                    + " no longer in the project. Choose who takes over, and restore again with the new assignees.");
        }
        departedByTask.forEach((taskId, departed) -> {
            departed.forEach(userId -> taskCommands.removeAssignee(taskId, userId));
            newAssignees.forEach(userId -> taskCommands.addAssignee(taskId, userId));
        });
    }

    // ---- helpers ----

    private static Optional<NotRestorable> whyNot(DeletedTask t) {
        if (t.projectDeletedAt() != null) {
            return Optional.of(NotRestorable.PROJECT_IN_RECYCLE_BIN);
        }
        if (t.parentDeletedAt() != null) {
            return Optional.of(NotRestorable.PARENT_IN_RECYCLE_BIN);
        }
        if (ProjectStatus.fromLabel("status", t.projectStatus()) == ProjectStatus.COMPLETED) {
            return Optional.of(NotRestorable.PROJECT_COMPLETED);
        }
        return Optional.empty();
    }

    /** Whole days left before it is removed for good, rounded up: an item with 20 hours left has 1 day (R20). */
    static int daysLeft(Instant deletedAt, Instant now) {
        long seconds = Duration.between(now, deletedAt.plus(RecycleBin.RETENTION)).getSeconds();
        return (int) Math.max(0, Math.ceilDiv(seconds, 86_400L));
    }

    private record Entry(Instant deletedAt, long id, RecycleItemView view) {
    }
}

package com.matriz.usecases;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectRole;
import com.matriz.projects.ProjectRecord;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.ConflictException;
import com.matriz.shared.ForbiddenException;
import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.NewTask;
import com.matriz.tasks.TaskCommands;
import com.matriz.tasks.TaskQueries;
import com.matriz.tasks.TaskRecord;
import com.matriz.tasks.TaskStatus;

/**
 * createTask, getTask and updateTask (F02, F03, F08; US02, US04, US05). The user comes from the
 * session. Rows are locked in a fixed order (the parent, then the task), so two requests about
 * the same task, or about a task and its subtasks, are done one after the other (R2 to R4, T13).
 */
@Service
public class TaskUseCases {

    static final int TITLE_MAX = 100;
    static final int DESCRIPTION_MAX = 2000;
    static final int BLOCK_REASON_MAX = 100;

    private final TaskQueries queries;
    private final TaskCommands commands;
    private final TaskRules rules;

    TaskUseCases(TaskQueries queries, TaskCommands commands, TaskRules rules) {
        this.queries = queries;
        this.commands = commands;
        this.rules = rules;
    }

    /** 404, 403 and R22, checked before the body is looked at. */
    public void requireCanCreateIn(AuthenticatedUser user, long projectId) {
        TaskRules.requireOpenForCreation(rules.loadProject(user, projectId));
    }

    /** 404, 403 and R27/R28, checked before the body is looked at. */
    public void requireCanEdit(AuthenticatedUser user, long taskId) {
        TaskRules.requireNotCompleted(rules.load(user, taskId).project());
    }

    @Transactional
    public TaskView createTask(AuthenticatedUser user, long projectId, NewTaskCommand c) {
        ProjectRecord project = rules.loadProject(user, projectId);
        TaskRules.requireOpenForCreation(project);

        if (c.assigneeIds().size() != new HashSet<>(c.assigneeIds()).size()) {
            throw new InvalidRequestException("An assignee is repeated.", "assigneeIds", "Each person once.");
        }
        TaskRecord parent = null;
        if (c.parentTaskId() != null) {
            // The parent row is locked: a subtask cannot appear while its parent's deadline or status changes (R2, R4)
            parent = queries.findByIdForUpdate(c.parentTaskId()).filter(p -> p.projectId() == projectId)
                    .orElseThrow(() -> new NotFoundException("The parent task was not found."));
            if (parent.isSubtask()) {
                throw new BusinessRuleException("PARENT_IS_SUBTASK", "A subtask cannot have subtasks.");
            }
            if (parent.statusValue().isFinal()) {
                throw new BusinessRuleException("PARENT_TASK_CLOSED",
                        "The parent task is closed. Reopen it before adding a subtask.");
            }
        }
        requireDeadlineNotInPast(project, c.deadline());
        if (parent != null && c.deadline().isAfter(parent.deadline())) {
            throw new BusinessRuleException("SUBTASK_DEADLINE_AFTER_PARENT",
                    "The deadline is later than the deadline of the parent task.");
        }
        c.assigneeIds().forEach(id -> rules.requireEligible(project, id));

        long id = commands.insert(new NewTask(projectId, c.parentTaskId(), c.title(), c.description(), c.deadline(),
                c.urgency(), c.importance()));
        c.assigneeIds().forEach(userId -> commands.addAssignee(id, userId));
        return rules.view(queries.findById(id).orElseThrow(), true);
    }

    public TaskView getTask(AuthenticatedUser user, long taskId) {
        return rules.view(rules.load(user, taskId).task(), true);
    }

    @Transactional
    public TaskView updateTask(AuthenticatedUser user, long taskId, TaskPatch patch) {
        TaskContext context = rules.load(user, taskId);
        ProjectRecord project = context.project();
        TaskRules.requireNotCompleted(project);

        // Lock the parent first, then the task: the same order as createTask and the other updates
        Optional<TaskRecord> parent = context.task().isSubtask()
                ? queries.findByIdForUpdate(context.task().parentTaskId()) : Optional.empty();
        TaskRecord task = queries.findByIdForUpdate(taskId)
                .orElseThrow(() -> new NotFoundException("The task was not found."));
        if (patch.version() != task.version()) { // R30, T13
            throw new ConflictException("TASK_CHANGED", "This task was changed by someone else. Reload it and try again.");
        }

        String title = patch.has("title") ? patch.title() : task.title();
        String description = patch.has("description") ? patch.description() : task.description();
        LocalDate deadline = patch.has("deadline") ? patch.deadline() : task.deadline();
        int urgency = patch.has("urgency") ? patch.urgency() : task.urgency();
        int importance = patch.has("importance") ? patch.importance() : task.importance();
        TaskStatus status = patch.has("status") ? TaskStatus.fromLabel("status", patch.status()) : task.statusValue();

        if (!deadline.equals(task.deadline())) { // R13, R2, R3: only when the deadline is changed
            requireDeadlineNotInPast(project, deadline);
            if (parent.isPresent() && deadline.isAfter(parent.get().deadline())) {
                throw new BusinessRuleException("SUBTASK_DEADLINE_AFTER_PARENT",
                        "The deadline is later than the deadline of the parent task.");
            }
            if (!task.isSubtask() && queries.latestSubtaskDeadline(taskId).filter(deadline::isBefore).isPresent()) {
                throw new BusinessRuleException("PARENT_DEADLINE_BEFORE_SUBTASK",
                        "A subtask has a later deadline. Review the subtasks first.");
            }
        }
        if (status != task.statusValue()) {
            requireStatusChangeAllowed(context.role(), task.statusValue(), status);
            if ((status == TaskStatus.DONE || status == TaskStatus.CANCELLED) && !task.isSubtask()
                    && queries.hasOpenSubtasks(taskId)) { // R4
                throw new BusinessRuleException("PARENT_HAS_OPEN_SUBTASKS",
                        "This task has subtasks that are still open. Close them first.");
            }
        }
        String blockReason = blockReasonFor(task, patch, status);
        Long originalTaskId = originalFor(task, patch, status, project);

        TaskRecord updated = new TaskRecord(taskId, task.projectId(), task.parentTaskId(), originalTaskId, title,
                description, status.label(), deadline, urgency, importance, blockReason, task.version() + 1);
        if (!commands.update(updated, task.version())) {
            throw new ConflictException("TASK_CHANGED", "This task was changed by someone else. Reload it and try again.");
        }
        return rules.view(updated, true);
    }

    /** P8: any member moves a task between statuses; reopening a final one is only for the manager. */
    private static void requireStatusChangeAllowed(ProjectRole role, TaskStatus from, TaskStatus to) {
        if (from.isFinal() && !to.isFinal() && role != ProjectRole.MANAGER) {
            throw new ForbiddenException("Only the manager of the project can reopen a closed task.");
        }
    }

    /** R6: a blocked task needs a reason; the reason belongs only to blocked tasks. */
    private static String blockReasonFor(TaskRecord task, TaskPatch patch, TaskStatus status) {
        if (status != TaskStatus.BLOCKED) {
            if (patch.has("blockReason") && patch.blockReason() != null) {
                throw new InvalidRequestException("The block reason is only for blocked tasks.", "blockReason",
                        "Only when the status is Blocked.");
            }
            return null;
        }
        String reason = patch.has("blockReason") ? patch.blockReason() : task.blockReason();
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("BLOCK_REASON_REQUIRED", "A blocked task needs a reason. Write why it is blocked.");
        }
        if (reason.length() > BLOCK_REASON_MAX) {
            throw new InvalidRequestException("The block reason is too long.", "blockReason",
                    "Longer than " + BLOCK_REASON_MAX + " characters.");
        }
        return reason;
    }

    /** R6: a duplicate needs its original: same project, not itself and not a duplicate itself. */
    private Long originalFor(TaskRecord task, TaskPatch patch, TaskStatus status, ProjectRecord project) {
        if (status != TaskStatus.DUPLICATE) {
            if (patch.has("originalTaskId") && patch.originalTaskId() != null) {
                throw new InvalidRequestException("The original task is only for duplicates.", "originalTaskId",
                        "Only when the status is Duplicate.");
            }
            return null;
        }
        Long original = patch.has("originalTaskId") ? patch.originalTaskId() : task.originalTaskId();
        if (original == null) {
            throw new BusinessRuleException("ORIGINAL_TASK_REQUIRED", "A duplicate needs the task it duplicates. Choose the original.");
        }
        boolean valid = original != task.id() && queries.findById(original)
                .filter(o -> o.projectId() == project.id() && o.statusValue() != TaskStatus.DUPLICATE).isPresent();
        if (!valid) {
            throw new BusinessRuleException("ORIGINAL_TASK_NOT_VALID",
                    "The original must be another task of this project that is not a duplicate.");
        }
        return original;
    }

    private void requireDeadlineNotInPast(ProjectRecord project, LocalDate deadline) {
        if (deadline.isBefore(rules.today(project))) { // R13: today is accepted
            throw new BusinessRuleException("DEADLINE_IN_PAST", "The deadline cannot be in the past.");
        }
    }
}

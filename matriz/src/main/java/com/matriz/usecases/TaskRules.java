package com.matriz.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.ProjectRole;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.ProjectStatus;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.Quadrant;
import com.matriz.tasks.TaskQueries;
import com.matriz.tasks.TaskRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Rules shared by the use cases of tasks and assignees: finding a task the user can reach, what
 * the state of the project allows (R22, R27, R28), who can be an assignee (R21) and the view of a task.
 */
@Component
class TaskRules {

    private final TaskQueries tasks;
    private final ProjectQueries projects;
    private final TeamRepository team;
    private final ProjectPermissions permissions;
    private final UserDirectory users;
    private final Clock clock;

    TaskRules(TaskQueries tasks, ProjectQueries projects, TeamRepository team, ProjectPermissions permissions,
            UserDirectory users, Clock clock) {
        this.tasks = tasks;
        this.projects = projects;
        this.team = team;
        this.permissions = permissions;
        this.users = users;
        this.clock = clock;
    }

    /** 404 if the task or its project does not exist (or was deleted); 403 if the user has no access (S1). */
    TaskContext load(AuthenticatedUser user, long taskId) {
        TaskRecord task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("The task was not found."));
        ProjectRecord project = projects.findById(task.projectId())
                .orElseThrow(() -> new NotFoundException("The task was not found."));
        ProjectRole role = permissions.requireAccess(user, project.id());
        return new TaskContext(task, project, role);
    }

    /** 404, 403 and R27/R28: the task of a project where changes are allowed. */
    TaskContext loadForChange(AuthenticatedUser user, long taskId) {
        TaskContext context = load(user, taskId);
        requireNotCompleted(context.project());
        return context;
    }

    ProjectRecord loadProject(AuthenticatedUser user, long projectId) {
        ProjectRecord project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("The project was not found."));
        permissions.requireAccess(user, projectId);
        return project;
    }

    /** R27, R28: in a completed project nothing is created, changed, assigned or removed. */
    static void requireNotCompleted(ProjectRecord project) {
        if (status(project) == ProjectStatus.COMPLETED) {
            throw new BusinessRuleException("PROJECT_COMPLETED", "The project is completed, so its tasks cannot be changed.");
        }
    }

    /** P37: the comments of a closed task or subtask (Done, Cancelled, Duplicate) are only read; reopening the task lifts it. */
    static void requireTaskNotClosed(TaskRecord task) {
        if (task.statusValue().isFinal()) {
            throw new BusinessRuleException("TASK_CLOSED",
                    "The task is closed, so its comments cannot be changed. Reopen the task first.");
        }
    }

    /** R22: tasks and subtasks are only created in an open project. */
    static void requireOpenForCreation(ProjectRecord project) {
        if (!status(project).isOpen()) {
            throw new BusinessRuleException("PROJECT_NOT_OPEN",
                    "Tasks are not created in a " + (status(project) == ProjectStatus.COMPLETED ? "completed" : "suspended")
                            + " project. Resume the project first.");
        }
    }

    /** R21: assignees are members of the project or its manager. */
    void requireEligible(ProjectRecord project, long userId) {
        boolean eligible = userId == project.managerId() || team.isMember(project.id(), userId);
        if (!eligible) {
            throw new BusinessRuleException("ASSIGNEE_NOT_MEMBER",
                    "This person is not a member of the project. Ask the manager to add them to the project first.");
        }
    }

    /** "Today" in the time zone of the project (P26): the earliest accepted deadline (R13). */
    LocalDate today(ProjectRecord project) {
        return LocalDate.now(clock.withZone(ZoneId.of(project.timezone())));
    }

    TaskView view(TaskRecord task, boolean withSubtasks) {
        List<TaskRecord> subtasks = withSubtasks && !task.isSubtask() ? tasks.subtasksOf(task.id()) : List.of();
        List<Long> ids = new ArrayList<>(List.of(task.id()));
        subtasks.forEach(s -> ids.add(s.id()));
        var assignees = tasks.assigneeIdsByTask(ids);
        var infos = users.findByIds(assignees.values().stream().flatMap(List::stream).distinct().toList());
        List<TaskView> subtaskViews = subtasks.stream().map(s -> toView(s, assignees.get(s.id()), infos, null)).toList();
        return toView(task, assignees.get(task.id()), infos, task.isSubtask() ? null : subtaskViews);
    }

    private static TaskView toView(TaskRecord t, List<Long> assigneeIds, java.util.Map<Long, UserInfo> infos,
            List<TaskView> subtasks) {
        List<UserInfo> assignees = assigneeIds == null ? List.of() : assigneeIds.stream().map(infos::get).toList();
        return new TaskView(t.id(), t.version(), t.projectId(), t.parentTaskId(), t.title(), t.description(), t.status(),
                t.deadline(), t.urgency(), t.importance(), Quadrant.of(t.urgency(), t.importance()).label(), t.blockReason(),
                t.originalTaskId(), assignees, subtasks);
    }

    /**
     * The views of main tasks, each with its subtasks inside (R18), keeping the order given. The
     * subtasks, the assignees and the people are read once for all of them, not once per task.
     */
    List<TaskView> viewsOfMainTasks(List<TaskRecord> mainTasks) {
        var subtasksByParent = tasks.subtasksOfAll(mainTasks.stream().map(TaskRecord::id).toList()).stream()
                .collect(java.util.stream.Collectors.groupingBy(TaskRecord::parentTaskId));
        List<Long> ids = new ArrayList<>(mainTasks.stream().map(TaskRecord::id).toList());
        subtasksByParent.values().forEach(list -> list.forEach(s -> ids.add(s.id())));
        var assignees = tasks.assigneeIdsByTask(ids);
        var infos = users.findByIds(assignees.values().stream().flatMap(List::stream).distinct().toList());
        return mainTasks.stream().map(t -> toView(t, assignees.get(t.id()), infos,
                subtasksByParent.getOrDefault(t.id(), List.of()).stream()
                        .map(s -> toView(s, assignees.get(s.id()), infos, null)).toList())).toList();
    }

    private static ProjectStatus status(ProjectRecord project) {
        return ProjectStatus.fromLabel("status", project.status());
    }
}

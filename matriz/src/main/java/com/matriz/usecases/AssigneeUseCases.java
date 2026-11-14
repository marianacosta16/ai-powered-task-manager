package com.matriz.usecases;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.TaskCommands;
import com.matriz.tasks.TaskQueries;
import com.matriz.tasks.TaskRecord;

/**
 * addAssignee, removeAssignee and listAssignableUsers (F02; US02). The row of the task is locked
 * while its assignees change, so two removals at the same time cannot leave it without assignees (R26).
 */
@Service
public class AssigneeUseCases {

    private final TaskQueries queries;
    private final TaskCommands commands;
    private final TaskRules rules;
    private final TeamRepository team;
    private final UserDirectory users;
    private final Locks locks;

    AssigneeUseCases(TaskQueries queries, TaskCommands commands, TaskRules rules, TeamRepository team,
            UserDirectory users, Locks locks) {
        this.queries = queries;
        this.commands = commands;
        this.rules = rules;
        this.team = team;
        this.users = users;
        this.locks = locks;
    }

    /** The members of the project and its manager (R21). */
    public List<UserInfo> listAssignableUsers(AuthenticatedUser user, long projectId) {
        ProjectRecord project = rules.loadProject(user, projectId);
        List<Long> ids = new ArrayList<>(team.memberIds(projectId));
        if (!ids.contains(project.managerId())) {
            ids.add(project.managerId());
        }
        var infos = users.findByIds(ids);
        return ids.stream().sorted().map(infos::get).toList();
    }

    /** The body is read only after 404, 403 and R27/R28 are checked (403 before 400). */
    @Transactional
    public List<UserInfo> addAssignee(AuthenticatedUser user, long taskId, Supplier<Long> body) {
        TaskContext context = rules.loadForChange(user, taskId);
        long userId = body.get();
        locks.people(List.of(userId)); // locks: people first, then tasks, always by id (see Locks)
        TaskRecord task = lock(taskId);
        rules.requireEligible(context.project(), userId); // R21 (scenario 2.9)
        commands.addAssignee(task.id(), userId);
        return assignees(task.id());
    }

    @Transactional
    public void removeAssignee(AuthenticatedUser user, long taskId, long userId) {
        rules.loadForChange(user, taskId);
        TaskRecord task = lock(taskId);
        List<Long> current = queries.assigneeIds(task.id());
        if (!current.contains(userId)) {
            throw new NotFoundException("This person is not an assignee of the task.");
        }
        if (current.size() == 1) { // R26 (scenario 2.11)
            throw new BusinessRuleException("LAST_ASSIGNEE",
                    "A task needs at least one assignee. Assign someone else first.");
        }
        commands.removeAssignee(task.id(), userId);
    }

    private TaskRecord lock(long taskId) {
        return locks.task(taskId).orElseThrow(() -> new NotFoundException("The task was not found."));
    }

    private List<UserInfo> assignees(long taskId) {
        List<Long> ids = queries.assigneeIds(taskId);
        var infos = users.findByIds(ids);
        return ids.stream().map(infos::get).toList();
    }
}

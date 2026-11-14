package com.matriz.usecases;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.OpenTaskCounts;
import com.matriz.tasks.TaskCommands;
import com.matriz.tasks.TaskQueries;

/**
 * Removing a member (R17, P24). Every open task and subtask of the member, also the shared ones,
 * needs a substitute; the tasks can be split among several substitutes; a substitute who is not in
 * the project yet is added to it (A7); nobody ends up with more than 5 open tasks in the project
 * (A21). Everything is done in one transaction: if a check fails nothing changes.
 *
 * <p>The checks run twice. First without locks, so a request that cannot work is refused before it
 * locks anybody. Then with the people and the tasks locked (in the order of {@link Locks}), because
 * the data may have changed meanwhile; this second pass is the one that counts.
 */
@Component
class MemberRemoval {

    static final int MAX_OPEN_TASKS_OF_A_SUBSTITUTE = 5; // R17, P24

    private final TeamRepository team;
    private final OpenTaskCounts openTasks;
    private final TaskQueries tasks;
    private final TaskCommands taskCommands;
    private final UserDirectory users;
    private final TeamRules rules;
    private final Locks locks;

    MemberRemoval(TeamRepository team, OpenTaskCounts openTasks, TaskQueries tasks, TaskCommands taskCommands,
            UserDirectory users, TeamRules rules, Locks locks) {
        this.team = team;
        this.openTasks = openTasks;
        this.tasks = tasks;
        this.taskCommands = taskCommands;
        this.users = users;
        this.rules = rules;
        this.locks = locks;
    }

    void remove(ProjectRecord project, long userId, List<TaskReassignment> reassignments) {
        long projectId = project.id();
        check(project, userId, reassignments, tasks.openTaskIdsOf(projectId, userId)); // without locks

        List<Long> substituteIds = substituteIds(reassignments);
        TreeSet<Long> people = new TreeSet<>(substituteIds);
        people.add(userId);
        locks.people(people); // the person removed and the substitutes, by id
        List<Long> lockedTasks = locks.openTasksOf(projectId, userId); // then the tasks, by id
        List<Long> newMembers = check(project, userId, reassignments, lockedTasks); // the check that counts

        reassignments.forEach(r -> taskCommands.replaceAssignee(r.taskId(), userId, r.newAssigneeId()));
        newMembers.forEach(id -> team.add(projectId, id));
        team.remove(projectId, userId);
    }

    /** All the checks. It changes nothing; it returns the substitutes who have to be added to the project. */
    private List<Long> check(ProjectRecord project, long userId, List<TaskReassignment> reassignments,
            List<Long> openTaskIds) {
        if (!team.isMember(project.id(), userId)) {
            throw new NotFoundException("The person is not a member of this project.");
        }
        requireEachOpenTaskNamedOnce(reassignments, openTaskIds);
        Map<Long, UserInfo> substitutes = users.findByIds(substituteIds(reassignments));
        requireSubstitutesCanTakeOver(userId, reassignments, substitutes, openTaskIds);
        List<Long> newMembers = substituteIds(reassignments).stream()
                .filter(id -> id != project.managerId() && !team.isMember(project.id(), id)).toList();
        newMembers.forEach(id -> rules.requireFreeToJoin(project, substitutes.get(id))); // R11, EC18
        requireWithinTheLimit(project.id(), reassignments, substitutes);
        return newMembers;
    }

    /** The list names each open task of the person exactly once (A23); a missing one needs a substitute (R17). */
    private void requireEachOpenTaskNamedOnce(List<TaskReassignment> reassignments, List<Long> openTaskIds) {
        Set<Long> named = new HashSet<>();
        for (TaskReassignment r : reassignments) {
            if (!openTaskIds.contains(r.taskId())) {
                throw new InvalidRequestException("A task in the list is not an open task of this person.", "reassignments",
                        "Task " + r.taskId() + " is not an open task of this person in this project.");
            }
            if (!named.add(r.taskId())) {
                throw new InvalidRequestException("A task is repeated in the list.", "reassignments",
                        "Task " + r.taskId() + " appears more than once.");
            }
        }
        if (named.size() < openTaskIds.size()) {
            throw new BusinessRuleException("REPLACEMENT_REQUIRED",
                    "This person has open tasks in the project. Choose a substitute for each of them before removing the person.");
        }
    }

    /** A substitute exists, is not the person removed and is not already responsible for that task (P24). */
    private void requireSubstitutesCanTakeOver(long userId, List<TaskReassignment> reassignments,
            Map<Long, UserInfo> substitutes, List<Long> openTaskIds) {
        var assignees = tasks.assigneeIdsByTask(openTaskIds);
        for (TaskReassignment r : reassignments) {
            if (!substitutes.containsKey(r.newAssigneeId()) || r.newAssigneeId() == userId
                    || assignees.getOrDefault(r.taskId(), List.of()).contains(r.newAssigneeId())) {
                throw new BusinessRuleException("REPLACEMENT_NOT_ELIGIBLE",
                        "Person " + r.newAssigneeId() + " cannot take over task " + r.taskId()
                                + ": choose someone who is not already responsible for it.");
            }
        }
    }

    /** Nobody ends up with more than 5 open tasks and subtasks in the project (R17, P24, A21). */
    private void requireWithinTheLimit(long projectId, List<TaskReassignment> reassignments,
            Map<Long, UserInfo> substitutes) {
        var current = openTasks.byAssignee(projectId);
        Map<Long, Integer> received = new HashMap<>();
        reassignments.forEach(r -> received.merge(r.newAssigneeId(), 1, Integer::sum));
        for (var entry : received.entrySet()) {
            if (current.getOrDefault(entry.getKey(), 0) + entry.getValue() > MAX_OPEN_TASKS_OF_A_SUBSTITUTE) {
                throw new BusinessRuleException("ASSIGNEE_LIMIT_EXCEEDED", substitutes.get(entry.getKey()).name()
                        + " would have more than " + MAX_OPEN_TASKS_OF_A_SUBSTITUTE
                        + " open tasks. Choose someone else or split the tasks.");
            }
        }
    }

    private static List<Long> substituteIds(List<TaskReassignment> reassignments) {
        return reassignments.stream().map(TaskReassignment::newAssigneeId).distinct().toList();
    }
}

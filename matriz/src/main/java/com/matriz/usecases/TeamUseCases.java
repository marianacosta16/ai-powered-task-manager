package com.matriz.usecases;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.Role;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.ProjectStatus;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.NotFoundException;
import com.matriz.tasks.OpenTaskCounts;
import com.matriz.tasks.TaskCommands;
import com.matriz.tasks.TaskQueries;

/**
 * The team of a project (F01, F06, US01): list, candidates, add and remove members. R11 compares
 * the team with the other projects and the open tasks come from the Tasks module, so the use case
 * coordinates them.
 */
@Service
public class TeamUseCases {

    static final int MAX_OPEN_TASKS_OF_A_SUBSTITUTE = 5; // R17, P24

    private final ProjectQueries projects;
    private final TeamRepository team;
    private final OpenTaskCounts openTasks;
    private final TaskQueries tasks;
    private final TaskCommands taskCommands;
    private final ProjectPermissions permissions;
    private final UserDirectory users;

    public TeamUseCases(ProjectQueries projects, TeamRepository team, OpenTaskCounts openTasks, TaskQueries tasks,
            TaskCommands taskCommands, ProjectPermissions permissions, UserDirectory users) {
        this.projects = projects;
        this.team = team;
        this.openTasks = openTasks;
        this.tasks = tasks;
        this.taskCommands = taskCommands;
        this.permissions = permissions;
        this.users = users;
    }

    /** The members (not the manager, M5) with their open tasks; the manager or a member of the project can see them. */
    public List<MemberView> listMembers(AuthenticatedUser user, long projectId) {
        ProjectRecord project = find(projectId);
        permissions.requireAccess(user, projectId);
        List<Long> ids = team.memberIds(projectId).stream().filter(id -> id != project.managerId()).toList();
        return memberViews(project, ids, openTasks.byAssignee(projectId));
    }

    /** Everyone not in the project yet; a person in another open project is unavailable (R11, scenario 1.5). */
    public List<CandidateView> listCandidateMembers(AuthenticatedUser user, long projectId) {
        ProjectRecord project = find(projectId);
        permissions.requireManager(user, projectId);
        Set<Long> inTeam = Set.copyOf(team.memberIds(projectId));
        List<UserInfo> candidates = users.findAll().stream()
                .filter(u -> u.id() != project.managerId() && !inTeam.contains(u.id())).toList();
        Set<Long> busy = isOpen(project)
                ? team.inOtherOpenProject(candidates.stream().map(UserInfo::id).toList(), projectId) : Set.of();
        return candidates.stream().map(u -> {
            boolean unavailable = busy.contains(u.id()) && !isManagerRole(u); // R11 does not apply to managers
            return new CandidateView(u, !unavailable, unavailable ? "IN_OTHER_OPEN_PROJECT" : null);
        }).toList();
    }

    @Transactional
    public MemberView addMember(AuthenticatedUser user, long projectId, long userId) {
        ProjectRecord project = find(projectId);
        permissions.requireManager(user, projectId);
        ProjectUseCases.requireNotCompleted(project); // R28
        // Lock the person first: the checks below and the insert must not be mixed with another
        // request about the same person (R11 across projects, and the same person added twice)
        UserInfo person = users.findForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("The person was not found."));
        if (userId == project.managerId() || team.isMember(projectId, userId)) {
            throw new BusinessRuleException("MEMBER_ALREADY_IN_PROJECT", "This person is already part of the project.");
        }
        // R11: one open project per member; managers are exempt, and a suspended project is not open (P4)
        if (isOpen(project) && !isManagerRole(person)
                && !team.inOtherOpenProject(List.of(userId), projectId).isEmpty()) {
            throw new BusinessRuleException("MEMBER_IN_OTHER_OPEN_PROJECT",
                    "This person is already in another open project.");
        }
        team.add(projectId, userId);
        return memberViews(project, List.of(userId), Map.of(userId, openTasks.forAssignee(projectId, userId))).get(0);
    }

    /**
     * Removes a member (R17, P24). Every open task and subtask of the member, also the shared ones,
     * needs a substitute in {@code reassignments}; the tasks can be split among several substitutes,
     * a substitute who is not in the project yet is added to it (A7), and nobody ends up with more
     * than 5 open tasks in this project (A21). Everything is done in one transaction: if a check fails
     * nothing changes.
     *
     * <p>Locks, here and in the other operations that assign people: the people first (by id), then
     * the tasks (by id). With one order, two requests never wait for each other in a circle.
     */
    @Transactional
    public void removeMember(AuthenticatedUser user, long projectId, long userId, List<TaskReassignment> reassignments) {
        ProjectRecord project = find(projectId);
        permissions.requireManager(user, projectId);
        ProjectUseCases.requireNotCompleted(project); // R28

        List<Long> substituteIds = reassignments.stream().map(TaskReassignment::newAssigneeId).distinct().toList();
        java.util.TreeSet<Long> people = new java.util.TreeSet<>(substituteIds);
        people.add(userId);
        people.forEach(users::findForUpdate); // the person removed and the substitutes, by id
        if (!team.isMember(projectId, userId)) {
            throw new NotFoundException("The person is not a member of this project.");
        }
        List<Long> openTaskIds = tasks.lockOpenTaskIdsOf(projectId, userId); // then the tasks, by id

        // The list must name each open task of the person exactly once (A23)
        Set<Long> named = new java.util.HashSet<>();
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

        // Who can take over: not the person removed, and not already responsible for that task (P24)
        var substitutes = users.findByIds(substituteIds);
        var assignees = tasks.assigneeIdsByTask(openTaskIds);
        for (TaskReassignment r : reassignments) {
            UserInfo substitute = substitutes.get(r.newAssigneeId());
            if (substitute == null || r.newAssigneeId() == userId
                    || assignees.getOrDefault(r.taskId(), List.of()).contains(r.newAssigneeId())) {
                throw new BusinessRuleException("REPLACEMENT_NOT_ELIGIBLE",
                        "Person " + r.newAssigneeId() + " cannot take over task " + r.taskId()
                                + ": choose someone who is not already responsible for it.");
            }
        }
        // A substitute who is not in the project yet is added to it, so the one open project per member
        // rule applies (R11, EC18); managers are exempt and a suspended project is not open (P4)
        List<Long> newMembers = substituteIds.stream()
                .filter(id -> id != project.managerId() && !team.isMember(projectId, id)).toList();
        for (Long id : newMembers) {
            if (isOpen(project) && !isManagerRole(substitutes.get(id))
                    && !team.inOtherOpenProject(List.of(id), projectId).isEmpty()) {
                throw new BusinessRuleException("MEMBER_IN_OTHER_OPEN_PROJECT",
                        substitutes.get(id).name() + " is already in another open project.");
            }
        }
        // Nobody ends up with more than 5 open tasks and subtasks in the project (R17, P24, A21)
        var current = openTasks.byAssignee(projectId);
        var received = new java.util.HashMap<Long, Integer>();
        reassignments.forEach(r -> received.merge(r.newAssigneeId(), 1, Integer::sum));
        for (var entry : received.entrySet()) {
            if (current.getOrDefault(entry.getKey(), 0) + entry.getValue() > MAX_OPEN_TASKS_OF_A_SUBSTITUTE) {
                throw new BusinessRuleException("ASSIGNEE_LIMIT_EXCEEDED", substitutes.get(entry.getKey()).name()
                        + " would have more than " + MAX_OPEN_TASKS_OF_A_SUBSTITUTE
                        + " open tasks. Choose someone else or split the tasks.");
            }
        }

        reassignments.forEach(r -> taskCommands.replaceAssignee(r.taskId(), userId, r.newAssigneeId()));
        newMembers.forEach(id -> team.add(projectId, id));
        team.remove(projectId, userId);
    }

    private List<MemberView> memberViews(ProjectRecord project, List<Long> ids, Map<Long, Integer> counts) {
        var infos = users.findByIds(ids);
        // The red label: a member of a resumed (open) project who is also in another open project (P22)
        Set<Long> flagged = isOpen(project) ? team.inOtherOpenProject(ids, project.id()) : Set.of();
        return ids.stream().map(infos::get)
                .map(u -> new MemberView(u.id(), u.name(), u.role(), counts.getOrDefault(u.id(), 0),
                        flagged.contains(u.id()) && !isManagerRole(u)))
                .toList();
    }

    private ProjectRecord find(long projectId) {
        return projects.findById(projectId).orElseThrow(() -> new NotFoundException("The project was not found."));
    }

    private static boolean isOpen(ProjectRecord project) {
        return ProjectStatus.fromLabel("status", project.status()).isOpen();
    }

    private static boolean isManagerRole(UserInfo user) {
        return Role.PROJECT_MANAGER.label().equals(user.role());
    }
}

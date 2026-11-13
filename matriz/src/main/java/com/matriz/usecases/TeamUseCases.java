package com.matriz.usecases;

import java.util.List;
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

/**
 * The team of a project (F01, F06, US01): list, candidates, add and remove members. R11 compares
 * the team with the other projects and the open tasks come from the Tasks module, so the use case
 * coordinates them.
 */
@Service
public class TeamUseCases {

    private final ProjectQueries projects;
    private final TeamRepository team;
    private final OpenTaskCounts openTasks;
    private final ProjectPermissions permissions;
    private final UserDirectory users;

    public TeamUseCases(ProjectQueries projects, TeamRepository team, OpenTaskCounts openTasks,
            ProjectPermissions permissions, UserDirectory users) {
        this.projects = projects;
        this.team = team;
        this.openTasks = openTasks;
        this.permissions = permissions;
        this.users = users;
    }

    /** The members (not the manager, M5) with their open tasks; the manager or a member of the project can see them. */
    public List<MemberView> listMembers(AuthenticatedUser user, long projectId) {
        ProjectRecord project = find(projectId);
        permissions.requireAccess(user, projectId);
        List<Long> ids = team.memberIds(projectId).stream().filter(id -> id != project.managerId()).toList();
        return memberViews(project, ids);
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
        UserInfo person = users.findByIds(List.of(userId)).get(userId);
        if (person == null) {
            throw new NotFoundException("The person was not found.");
        }
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
        return memberViews(project, List.of(userId)).get(0);
    }

    /**
     * Removes a member without open tasks. A member with open tasks needs substitutes (R17), which
     * arrive with the tasks (increment 3); until then the removal is refused.
     */
    @Transactional
    public void removeMember(AuthenticatedUser user, long projectId, long userId, int reassignments) {
        ProjectRecord project = find(projectId);
        permissions.requireManager(user, projectId);
        ProjectUseCases.requireNotCompleted(project); // R28
        if (!team.isMember(projectId, userId)) {
            throw new NotFoundException("The person is not a member of this project.");
        }
        if (openTasks.forAssignee(projectId, userId) > 0) {
            throw new BusinessRuleException("REPLACEMENT_REQUIRED",
                    "This person has open tasks in the project. Reassign them to someone else before removing them.");
        }
        if (reassignments > 0) {
            throw new InvalidRequestException("There is nothing to reassign.", "reassignments",
                    "This person has no open tasks to reassign.");
        }
        team.remove(projectId, userId);
    }

    private List<MemberView> memberViews(ProjectRecord project, List<Long> ids) {
        var infos = users.findByIds(ids);
        var counts = openTasks.byAssignee(project.id());
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

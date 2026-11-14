package com.matriz.usecases;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matriz.access.AuthenticatedUser;
import com.matriz.access.ProjectPermissions;
import com.matriz.access.UserDirectory;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectQueries;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;
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
    private final TeamRules rules;
    private final MemberRemoval removal;
    private final Locks locks;

    TeamUseCases(ProjectQueries projects, TeamRepository team, OpenTaskCounts openTasks, ProjectPermissions permissions,
            UserDirectory users, TeamRules rules, MemberRemoval removal, Locks locks) {
        this.projects = projects;
        this.team = team;
        this.openTasks = openTasks;
        this.permissions = permissions;
        this.users = users;
        this.rules = rules;
        this.removal = removal;
        this.locks = locks;
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
        Set<Long> busy = TeamRules.isOpen(project)
                ? team.inOtherOpenProject(candidates.stream().map(UserInfo::id).toList(), projectId) : Set.of();
        return candidates.stream().map(u -> {
            boolean unavailable = busy.contains(u.id()) && !TeamRules.isManagerRole(u); // R11 does not apply to managers
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
        UserInfo person = locks.person(userId)
                .orElseThrow(() -> new NotFoundException("The person was not found."));
        if (userId == project.managerId() || team.isMember(projectId, userId)) {
            throw new BusinessRuleException("MEMBER_ALREADY_IN_PROJECT", "This person is already part of the project.");
        }
        rules.requireFreeToJoin(project, person); // R11
        team.add(projectId, userId);
        return memberViews(project, List.of(userId), Map.of(userId, openTasks.forAssignee(projectId, userId))).get(0);
    }

    /** Removes a member, passing the open tasks to substitutes (R17, P24); see {@link MemberRemoval}. */
    @Transactional
    public void removeMember(AuthenticatedUser user, long projectId, long userId, List<TaskReassignment> reassignments) {
        ProjectRecord project = find(projectId);
        permissions.requireManager(user, projectId);
        ProjectUseCases.requireNotCompleted(project); // R28
        removal.remove(project, userId, reassignments);
    }

    private List<MemberView> memberViews(ProjectRecord project, List<Long> ids, Map<Long, Integer> counts) {
        var infos = users.findByIds(ids);
        // The red label: a member of a resumed (open) project who is also in another open project (P22)
        Set<Long> flagged = TeamRules.isOpen(project) ? team.inOtherOpenProject(ids, project.id()) : Set.of();
        return ids.stream().map(infos::get)
                .map(u -> new MemberView(u.id(), u.name(), u.role(), counts.getOrDefault(u.id(), 0),
                        flagged.contains(u.id()) && !TeamRules.isManagerRole(u)))
                .toList();
    }

    private ProjectRecord find(long projectId) {
        return projects.findById(projectId).orElseThrow(() -> new NotFoundException("The project was not found."));
    }


}

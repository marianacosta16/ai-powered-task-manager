package com.matriz.usecases;

import java.util.List;

import org.springframework.stereotype.Component;

import com.matriz.access.Role;
import com.matriz.access.UserInfo;
import com.matriz.projects.ProjectRecord;
import com.matriz.projects.ProjectStatus;
import com.matriz.projects.TeamRepository;
import com.matriz.shared.BusinessRuleException;

/** Rules about who can join a team, shared by the operations that add someone to a project. */
@Component
class TeamRules {

    private final TeamRepository team;

    TeamRules(TeamRepository team) {
        this.team = team;
    }

    /** An open project is not completed and not suspended (P4). */
    static boolean isOpen(ProjectRecord project) {
        return ProjectStatus.fromLabel("status", project.status()).isOpen();
    }

    static boolean isManagerRole(UserInfo user) {
        return Role.PROJECT_MANAGER.label().equals(user.role());
    }

    /**
     * R11: a member is in one open project at a time. It is only checked when the person joins an
     * open project; managers are exempt and a suspended project is not open (P4).
     */
    void requireFreeToJoin(ProjectRecord project, UserInfo person) {
        if (isOpen(project) && !isManagerRole(person)
                && !team.inOtherOpenProject(List.of(person.id()), project.id()).isEmpty()) {
            throw new BusinessRuleException("MEMBER_IN_OTHER_OPEN_PROJECT",
                    person.name() + " is already in another open project.");
        }
    }
}

package com.matriz.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.access.AuthenticatedUser;
import com.matriz.usecases.CandidateView;
import com.matriz.usecases.MemberView;
import com.matriz.usecases.TaskReassignment;
import com.matriz.usecases.TeamUseCases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

/** listMembers, addMember, removeMember and listCandidateMembers. */
@Validated
@RestController
class TeamController {

    private final TeamUseCases team;

    TeamController(TeamUseCases team) {
        this.team = team;
    }

    @GetMapping("/projects/{projectId}/members")
    List<MemberView> listMembers(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Min(1) long projectId) {
        return team.listMembers(user, projectId);
    }

    @PostMapping("/projects/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    MemberView addMember(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long projectId,
            @Valid @RequestBody AddMemberRequest body) {
        return team.addMember(user, projectId, body.userId());
    }

    @PostMapping("/projects/{projectId}/members/{userId}/removal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long projectId,
            @PathVariable @Min(1) long userId, @Valid @RequestBody MemberRemovalRequest body) {
        team.removeMember(user, projectId, userId, body.reassignments().stream()
                .map(r -> new TaskReassignment(r.taskId(), r.newAssigneeId())).toList());
    }

    @GetMapping("/projects/{projectId}/candidate-members")
    List<CandidateView> listCandidateMembers(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Min(1) long projectId) {
        return team.listCandidateMembers(user, projectId);
    }
}

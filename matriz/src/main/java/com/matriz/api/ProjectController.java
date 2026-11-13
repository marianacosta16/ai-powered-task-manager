package com.matriz.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.access.AuthenticatedUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.usecases.ProjectReadUseCases;
import com.matriz.usecases.ProjectUseCases;
import com.matriz.usecases.ProjectSummaryView;
import com.matriz.usecases.ProjectView;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * listProjects and getProject. The user always comes from the session; nothing in the request
 * says who is asking.
 */
@Validated
@RestController
class ProjectController {

    private final ProjectReadUseCases useCases;
    private final ProjectUseCases projectUseCases;

    ProjectController(ProjectReadUseCases useCases, ProjectUseCases projectUseCases) {
        this.useCases = useCases;
        this.projectUseCases = projectUseCases;
    }

    /** The creator comes from the session; the manager is the one the creator indicates (R19). */
    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    ProjectView createProject(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProjectCreateRequest body) {
        return projectUseCases.createProject(user, body.toCommand());
    }

    @PatchMapping("/projects/{projectId}")
    ProjectView updateProject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long projectId,
            @RequestBody JsonNode body) {
        return projectUseCases.updateProject(user, projectId, ProjectPatchParser.parse(body));
    }

    @GetMapping("/projects")
    List<ProjectSummaryView> listProjects(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Size(min = 1, max = 100) String search) {
        return useCases.listProjects(user, search);
    }

    @GetMapping("/projects/{projectId}")
    ProjectView getProject(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Min(1) long projectId) {
        return useCases.getProject(user, projectId);
    }
}

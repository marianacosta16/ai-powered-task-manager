package com.matriz.api;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.access.AuthenticatedUser;
import com.matriz.usecases.ProjectReadUseCases;
import com.matriz.usecases.ProjectSummaryView;
import com.matriz.usecases.ProjectView;

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

    ProjectController(ProjectReadUseCases useCases) {
        this.useCases = useCases;
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

package com.matriz.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.access.AuthenticatedUser;
import com.matriz.usecases.RecycleItemView;
import com.matriz.usecases.RecycleUseCases;

import jakarta.validation.constraints.Min;

/**
 * deleteProject, deleteTask, listRecycleBin, restoreProject and restoreTask. The user comes from the session;
 * the body of restoreTask is read after the use case has checked 404 and 403 (403 before 400).
 */
@Validated
@RestController
class RecycleController {

    private final RecycleUseCases recycle;

    RecycleController(RecycleUseCases recycle) {
        this.recycle = recycle;
    }

    @DeleteMapping("/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteProject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long projectId) {
        recycle.deleteProject(user, projectId);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTask(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId) {
        recycle.deleteTask(user, taskId);
    }

    @GetMapping("/recycle-bin")
    List<RecycleItemView> listRecycleBin(@AuthenticationPrincipal AuthenticatedUser user) {
        return recycle.listRecycleBin(user);
    }

    @PostMapping("/recycle-bin/projects/{projectId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restoreProject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long projectId) {
        recycle.restoreProject(user, projectId);
    }

    @PostMapping("/recycle-bin/tasks/{taskId}/restore")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restoreTask(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId,
            @RequestBody(required = false) JsonNode body) {
        recycle.restoreTask(user, taskId, () -> RestoreTaskParser.parse(body));
    }
}

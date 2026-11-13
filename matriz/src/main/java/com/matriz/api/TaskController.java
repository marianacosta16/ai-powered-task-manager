package com.matriz.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.access.AuthenticatedUser;
import com.matriz.access.UserInfo;
import com.matriz.usecases.AssigneeUseCases;
import com.matriz.usecases.TaskUseCases;
import com.matriz.usecases.TaskView;

import jakarta.validation.constraints.Min;

/**
 * createTask, getTask, updateTask, addAssignee, removeAssignee and listAssignableUsers. The user
 * comes from the session. For the operations with a body, the use case checks who may do it (404,
 * 403 and the state of the project) before it reads the body, so someone who may not gets no hints
 * about the values.
 */
@Validated
@RestController
class TaskController {

    private final TaskUseCases tasks;
    private final AssigneeUseCases assignees;
    private final RequestValidator validator;

    TaskController(TaskUseCases tasks, AssigneeUseCases assignees, RequestValidator validator) {
        this.tasks = tasks;
        this.assignees = assignees;
        this.validator = validator;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    TaskView createTask(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long projectId,
            @RequestBody TaskCreateRequest body) {
        return tasks.createTask(user, projectId, () -> {
            validator.validate(body);
            return body.toCommand();
        });
    }

    @GetMapping("/tasks/{taskId}")
    TaskView getTask(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId) {
        return tasks.getTask(user, taskId);
    }

    @PatchMapping("/tasks/{taskId}")
    TaskView updateTask(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId,
            @RequestBody JsonNode body) {
        return tasks.updateTask(user, taskId, () -> TaskPatchParser.parse(body));
    }

    @PostMapping("/tasks/{taskId}/assignees")
    List<UserInfo> addAssignee(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId,
            @RequestBody AddAssigneeRequest body) {
        return assignees.addAssignee(user, taskId, () -> {
            validator.validate(body);
            return body.userId();
        });
    }

    @DeleteMapping("/tasks/{taskId}/assignees/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeAssignee(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable @Min(1) long taskId,
            @PathVariable @Min(1) long userId) {
        assignees.removeAssignee(user, taskId, userId);
    }

    @GetMapping("/projects/{projectId}/assignable-users")
    List<UserInfo> listAssignableUsers(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @Min(1) long projectId) {
        return assignees.listAssignableUsers(user, projectId);
    }
}

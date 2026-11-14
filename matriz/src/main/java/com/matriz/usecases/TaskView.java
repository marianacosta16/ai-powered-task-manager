package com.matriz.usecases;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matriz.access.UserInfo;

/**
 * Task of the contract: a task or a subtask, with its assignees; a main task also lists its subtasks (R18).
 * {@code originalEliminated} is true for a duplicate whose original was eliminated for good (A41).
 */
public record TaskView(long id, int version, long projectId, Long parentTaskId, String title, String description,
        String status, LocalDate deadline, int urgency, int importance, String quadrant, String blockReason,
        Long originalTaskId, boolean originalEliminated, List<UserInfo> assignees,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<TaskView> subtasks) {
}

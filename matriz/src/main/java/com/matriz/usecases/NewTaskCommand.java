package com.matriz.usecases;

import java.time.LocalDate;
import java.util.List;

/** What the client sends to create a task or a subtask (createTask). */
public record NewTaskCommand(String title, String description, LocalDate deadline, int urgency, int importance,
        List<Long> assigneeIds, Long parentTaskId) {
}

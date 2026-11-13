package com.matriz.api;

import java.time.LocalDate;
import java.util.List;

import com.matriz.usecases.NewTaskCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of createTask (TaskCreate in the contract). */
record TaskCreateRequest(
        @NotNull(message = "Is required.") @Size(min = 1, max = 100, message = "Must have 1 to 100 characters.") String title,
        @Size(max = 2000, message = "Longer than 2000 characters.") String description,
        @NotNull(message = "Is required.") LocalDate deadline,
        @NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 to 4.") @Max(value = 4, message = "Must be 1 to 4.") Integer urgency,
        @NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 to 4.") @Max(value = 4, message = "Must be 1 to 4.") Integer importance,
        @NotEmpty(message = "At least one assignee is required.") List<@NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 or more.") Long> assigneeIds,
        @Min(value = 1, message = "Must be 1 or more.") Long parentTaskId) {

    NewTaskCommand toCommand() {
        return new NewTaskCommand(title, description, deadline, urgency, importance, assigneeIds, parentTaskId);
    }
}

package com.matriz.api;

import java.time.LocalDate;
import java.time.LocalTime;

import com.matriz.shared.Limits;
import com.matriz.usecases.NewProjectCommand;

import jakarta.validation.constraints.NotNull;

/**
 * Body of createProject (ProjectCreate in the contract). The manager is not in it: it is the creator, who comes from
 * the session (R19). A {@code managerId} in the body is refused as an unknown field.
 */
record ProjectCreateRequest(
        @NotNull(message = "Is required.") @Chars(min = Limits.PROJECT_NAME_MIN, max = Limits.PROJECT_NAME_MAX, notBlank = true, message = Limits.PROJECT_NAME_MESSAGE) String name,
        @Chars(max = Limits.PROJECT_DESCRIPTION_MAX, message = Limits.PROJECT_DESCRIPTION_MESSAGE) String description,
        @NotNull(message = "Is required.") LocalDate startDate,
        LocalTime startTime,
        @NotNull(message = "Is required.") LocalDate endDate,
        LocalTime endTime,
        String timezone,
        @NotNull(message = "Is required.") String phase,
        @NotNull(message = "Is required.") String status) {

    NewProjectCommand toCommand() {
        return new NewProjectCommand(name, description, startDate, startTime, endDate, endTime, timezone, phase,
                status);
    }
}

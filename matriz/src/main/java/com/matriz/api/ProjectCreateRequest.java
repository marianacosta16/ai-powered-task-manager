package com.matriz.api;

import java.time.LocalDate;
import java.time.LocalTime;

import com.matriz.usecases.NewProjectCommand;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Body of createProject (ProjectCreate in the contract). */
record ProjectCreateRequest(
        @NotNull(message = "Is required.") @Chars(min = 1, max = 100, message = "Must have 1 to 100 characters.") String name,
        @Chars(max = 2000, message = "Longer than 2000 characters.") String description,
        @NotNull(message = "Is required.") LocalDate startDate,
        LocalTime startTime,
        @NotNull(message = "Is required.") LocalDate endDate,
        LocalTime endTime,
        String timezone,
        @NotNull(message = "Is required.") String phase,
        @NotNull(message = "Is required.") String status,
        @NotNull(message = "Is required.") @Min(value = 1, message = "Must be 1 or more.") Long managerId) {

    NewProjectCommand toCommand() {
        return new NewProjectCommand(name, description, startDate, startTime, endDate, endTime, timezone, phase,
                status, managerId);
    }
}

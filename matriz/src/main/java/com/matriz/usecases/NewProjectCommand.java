package com.matriz.usecases;

import java.time.LocalDate;
import java.time.LocalTime;

/** What the client sends to create a project (createProject). Phase and status are the labels of the contract. */
public record NewProjectCommand(String name, String description, LocalDate startDate, LocalTime startTime,
        LocalDate endDate, LocalTime endTime, String timezone, String phase, String status, long managerId) {
}

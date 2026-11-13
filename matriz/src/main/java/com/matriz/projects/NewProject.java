package com.matriz.projects;

import java.time.LocalDate;
import java.time.LocalTime;

/** Data of a project to insert; the values have been validated. */
public record NewProject(String name, String description, LocalDate startDate, LocalTime startTime,
        LocalDate endDate, LocalTime endTime, String timezone, String phase, String status, long managerId) {
}

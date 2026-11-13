package com.matriz.projects;

import java.time.LocalDate;
import java.time.LocalTime;

/** A project as stored (docs/database.md, PROJECT). The manager is held by identifier only. */
public record ProjectRecord(long id, String name, String description, LocalDate startDate, LocalTime startTime,
        LocalDate endDate, LocalTime endTime, String timezone, String phase, String status, long managerId) {
}

package com.matriz.usecases;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * The fields to change in updateProject. A field that is absent is kept; a field that is present
 * with null is cleared (description, start time, end time).
 */
public record ProjectPatch(Set<String> present, String name, String description, LocalDate startDate,
        LocalTime startTime, LocalDate endDate, LocalTime endTime, String timezone, String phase, String status,
        Long managerId) {

    public boolean has(String field) {
        return present.contains(field);
    }
}

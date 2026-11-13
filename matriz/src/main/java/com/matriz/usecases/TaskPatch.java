package com.matriz.usecases;

import java.time.LocalDate;
import java.util.Set;

/**
 * The fields to change in updateTask, and the version of the task that the client read (R30).
 * A field that is absent is kept; a field that is present with null is cleared (description,
 * blockReason, originalTaskId).
 */
public record TaskPatch(int version, Set<String> present, String title, String description, String status,
        LocalDate deadline, Integer urgency, Integer importance, String blockReason, Long originalTaskId) {

    public boolean has(String field) {
        return present.contains(field);
    }
}

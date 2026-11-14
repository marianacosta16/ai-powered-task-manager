package com.matriz.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.shared.InvalidRequestException;
import com.matriz.usecases.ProjectPatch;

/**
 * Reads the body of updateProject. It needs to tell an absent field (kept) from a null one
 * (cleared), which a plain object cannot, and it refuses unknown fields (additionalProperties: false).
 */
final class ProjectPatchParser {

    private static final Set<String> FIELDS = Set.of("name", "description", "startDate", "startTime", "endDate",
            "endTime", "timezone", "phase", "status", "managerId");

    private ProjectPatchParser() {
    }

    static ProjectPatch parse(JsonNode body) {
        if (body == null || !body.isObject() || body.isEmpty()) {
            throw new InvalidRequestException("There is nothing to change.", "body", "Send at least one field to change.");
        }
        Set<String> present = new HashSet<>();
        for (Iterator<String> names = body.fieldNames(); names.hasNext();) {
            String field = names.next();
            if (!FIELDS.contains(field)) {
                throw new InvalidRequestException("The request is not valid.", field, "Unknown field.");
            }
            present.add(field);
        }
        return new ProjectPatch(present,
                text(body, "name", false, 100), text(body, "description", true, 2000),
                date(body, "startDate", false), time(body, "startTime"), date(body, "endDate", false),
                time(body, "endTime"), text(body, "timezone", false, 100),
                text(body, "phase", false, 100), text(body, "status", false, 100), managerId(body));
    }

    private static String text(JsonNode body, String field, boolean nullable, int max) {
        JsonNode node = body.get(field);
        if (node == null) {
            return null;
        }
        if (node.isNull() && nullable) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalid(field, "Must be text.");
        }
        String value = node.asText();
        if (value.isEmpty() && !"description".equals(field)) {
            throw invalid(field, "Must have at least 1 character.");
        }
        if (value.codePointCount(0, value.length()) > max) { // characters, not UTF-16 units (R14)
            throw invalid(field, "Longer than " + max + " characters.");
        }
        return value;
    }

    private static LocalDate date(JsonNode body, String field, boolean nullable) {
        JsonNode node = body.get(field);
        if (node == null || (node.isNull() && nullable)) {
            return null;
        }
        try {
            return LocalDate.parse(node.asText());
        } catch (DateTimeParseException ex) {
            throw invalid(field, "Must be a date like 2026-01-31.");
        }
    }

    private static LocalTime time(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return LocalTime.parse(node.asText());
        } catch (DateTimeParseException ex) {
            throw invalid(field, "Must be a time like 09:30:00.");
        }
    }

    private static Long managerId(JsonNode body) {
        JsonNode node = body.get("managerId");
        if (node == null) {
            return null;
        }
        if (!node.isIntegralNumber() || node.asLong() < 1) {
            throw invalid("managerId", "Must be a whole number, 1 or more.");
        }
        return node.asLong();
    }

    private static InvalidRequestException invalid(String field, String issue) {
        return new InvalidRequestException("The value of '" + field + "' is not valid.", field, issue);
    }
}

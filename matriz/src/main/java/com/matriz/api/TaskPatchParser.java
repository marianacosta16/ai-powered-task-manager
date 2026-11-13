package com.matriz.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.shared.InvalidRequestException;
import com.matriz.usecases.TaskPatch;

/**
 * Reads the body of updateTask: the version that was read and the fields to change. It tells an
 * absent field (kept) from a null one (cleared), and refuses unknown fields (additionalProperties: false).
 */
final class TaskPatchParser {

    private static final Set<String> FIELDS = Set.of("version", "title", "description", "status", "deadline",
            "urgency", "importance", "blockReason", "originalTaskId");

    private TaskPatchParser() {
    }

    static TaskPatch parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalid("body", "Send an object with the version and the fields to change.");
        }
        Set<String> present = new HashSet<>();
        for (Iterator<String> names = body.fieldNames(); names.hasNext();) {
            String field = names.next();
            if (!FIELDS.contains(field)) {
                throw invalid(field, "Unknown field.");
            }
            present.add(field);
        }
        JsonNode version = body.get("version");
        if (version == null || !version.isIntegralNumber() || version.asLong() < 1 || version.asLong() > Integer.MAX_VALUE) {
            throw invalid("version", "Is required: the version of the task that was read.");
        }
        present.remove("version");
        if (present.isEmpty()) {
            throw invalid("body", "Send at least one field to change.");
        }
        return new TaskPatch(version.asInt(), present,
                text(body, "title", false, 100), text(body, "description", true, 2000), text(body, "status", false, 100),
                date(body), level(body, "urgency"), level(body, "importance"), text(body, "blockReason", true, Integer.MAX_VALUE),
                originalTaskId(body));
    }

    private static String text(JsonNode body, String field, boolean nullable, int max) {
        JsonNode node = body.get(field);
        if (node == null || (node.isNull() && nullable)) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalid(field, "Must be text.");
        }
        String value = node.asText();
        if (value.isEmpty() && "title".equals(field)) {
            throw invalid(field, "Must have at least 1 character.");
        }
        if (value.length() > max) {
            throw invalid(field, "Longer than " + max + " characters.");
        }
        return value;
    }

    private static LocalDate date(JsonNode body) {
        JsonNode node = body.get("deadline");
        if (node == null) {
            return null;
        }
        try {
            return LocalDate.parse(node.asText());
        } catch (DateTimeParseException ex) {
            throw invalid("deadline", "Must be a date like 2026-12-15.");
        }
    }

    private static Integer level(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null) {
            return null;
        }
        if (!node.isIntegralNumber() || node.asInt() < 1 || node.asInt() > 4) {
            throw invalid(field, "Must be 1 to 4.");
        }
        return node.asInt();
    }

    private static Long originalTaskId(JsonNode body) {
        JsonNode node = body.get("originalTaskId");
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isIntegralNumber() || node.asLong() < 1) {
            throw invalid("originalTaskId", "Must be a whole number, 1 or more.");
        }
        return node.asLong();
    }

    private static InvalidRequestException invalid(String field, String issue) {
        return new InvalidRequestException("The value of '" + field + "' is not valid.", field, issue);
    }
}

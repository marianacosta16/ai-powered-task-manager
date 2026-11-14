package com.matriz.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.shared.InvalidRequestException;

/** The optional body of restoreTask: {@code {"assigneeIds": [...]}}, the people who take over (P36). No body gives null. */
final class RestoreTaskParser {

    private RestoreTaskParser() {
    }

    static List<Long> parse(JsonNode body) {
        if (body == null || body.isNull() || (body.isObject() && body.isEmpty())) {
            return null;
        }
        if (!body.isObject() || body.size() != 1 || !body.has("assigneeIds")) {
            throw new InvalidRequestException("The request is not valid.", "body", "Only assigneeIds is accepted.");
        }
        JsonNode ids = body.get("assigneeIds");
        if (!ids.isArray()) {
            throw new InvalidRequestException("The assignees are not valid.", "assigneeIds", "Must be a list of identifiers.");
        }
        List<Long> result = new ArrayList<>();
        for (JsonNode id : ids) {
            if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() < 1) {
                throw new InvalidRequestException("The assignees are not valid.", "assigneeIds", "Each one must be a whole number from 1.");
            }
            result.add(id.asLong());
        }
        return result;
    }
}

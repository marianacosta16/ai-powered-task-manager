package com.matriz.api;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.matriz.shared.InvalidRequestException;

/**
 * Reads the body of addComment and editComment (CommentWrite in the contract): only the text.
 * The use case calls it after it has checked who may do it, so someone who may not comment gets
 * no hints about the body (403 before 400). The author and the date are never accepted (R24).
 */
final class CommentBodyParser {

    static final int MAX_CHARACTERS = 2000; // R14, P28

    private CommentBodyParser() {
    }

    static String parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalid("body", "Send an object with the content.");
        }
        for (Iterator<String> names = body.fieldNames(); names.hasNext();) {
            String field = names.next();
            if (!"content".equals(field)) {
                throw invalid(field, "Unknown field.");
            }
        }
        JsonNode node = body.get("content");
        if (node == null || node.isNull()) {
            throw invalid("content", "Is required.");
        }
        if (!node.isTextual()) {
            throw invalid("content", "Must be text.");
        }
        String content = node.asText();
        if (content.isBlank()) { // A25
            throw invalid("content", "Is required.");
        }
        if (content.codePointCount(0, content.length()) > MAX_CHARACTERS) {
            throw invalid("content", "Longer than " + MAX_CHARACTERS + " characters.");
        }
        return content;
    }

    private static InvalidRequestException invalid(String field, String issue) {
        return new InvalidRequestException("The value of '" + field + "' is not valid.", field, issue);
    }
}

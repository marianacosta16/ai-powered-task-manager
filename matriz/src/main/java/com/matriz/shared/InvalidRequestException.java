package com.matriz.shared;

/** A value of the request is missing, malformed or out of range (HTTP 400, code VALIDATION_ERROR). */
public class InvalidRequestException extends RuntimeException {

    private final String field;
    private final String issue;

    public InvalidRequestException(String message, String field, String issue) {
        super(message);
        this.field = field;
        this.issue = issue;
    }

    public String field() {
        return field;
    }

    public String issue() {
        return issue;
    }
}

package com.matriz.api;

import java.util.List;

/** The body of a request has values that are missing or not valid (HTTP 400, code VALIDATION_ERROR). */
class RequestValidationException extends RuntimeException {

    private final List<ErrorBody.Detail> details;

    RequestValidationException(List<ErrorBody.Detail> details) {
        super("Invalid request body");
        this.details = details;
    }

    List<ErrorBody.Detail> details() {
        return details;
    }
}

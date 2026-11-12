package com.matriz.shared;

/** The user has no access to the project or the action (HTTP 403, code FORBIDDEN). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

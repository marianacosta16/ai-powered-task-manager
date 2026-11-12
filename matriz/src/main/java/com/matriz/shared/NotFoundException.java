package com.matriz.shared;

/** The resource does not exist or was deleted (HTTP 404, code NOT_FOUND). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

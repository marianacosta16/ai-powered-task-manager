package com.matriz.shared;

/** The request is well formed but breaks a business rule (HTTP 422). The code says which one. */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

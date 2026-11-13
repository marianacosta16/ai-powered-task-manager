package com.matriz.api;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Validates a request body on demand, for the operations that must check who is asking (403)
 * before they look at the values (400).
 */
@Component
class RequestValidator {

    private final Validator validator;

    RequestValidator(Validator validator) {
        this.validator = validator;
    }

    void validate(Object body) {
        List<ErrorBody.Detail> details = validator.validate(body).stream()
                .sorted(Comparator.comparing((ConstraintViolation<Object> v) -> v.getPropertyPath().toString()))
                .map(v -> new ErrorBody.Detail(v.getPropertyPath().toString(), v.getMessage())).toList();
        if (!details.isEmpty()) {
            throw new RequestValidationException(details);
        }
    }
}

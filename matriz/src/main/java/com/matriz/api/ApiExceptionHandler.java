package com.matriz.api;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.matriz.shared.ForbiddenException;
import com.matriz.shared.NotFoundException;

import jakarta.validation.ConstraintViolationException;

/** Errors of the contract: code + message, with the field when a value is invalid. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    static final String INVALID_CREDENTIALS =
            "Credentials are not correct. Check your username and password and try again.";

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorBody> invalidCredentials(AuthenticationException ex) {
        return reply(HttpStatus.UNAUTHORIZED, ErrorBody.of("INVALID_CREDENTIALS", INVALID_CREDENTIALS));
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ErrorBody> forbidden(ForbiddenException ex) {
        return reply(HttpStatus.FORBIDDEN, ErrorBody.of("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorBody> notFound(NotFoundException ex) {
        return reply(HttpStatus.NOT_FOUND, ErrorBody.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorBody> invalidBody(MethodArgumentNotValidException ex) {
        List<ErrorBody.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErrorBody.Detail(e.getField(), "Is required.")).toList();
        return validation("A required field is missing. Fill it in and try again.", details);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class, MethodArgumentTypeMismatchException.class })
    ResponseEntity<ErrorBody> invalidRequest(Exception ex) {
        return validation("The request is not valid. Check the values and try again.", null);
    }

    private ResponseEntity<ErrorBody> validation(String message, List<ErrorBody.Detail> details) {
        return reply(HttpStatus.BAD_REQUEST, new ErrorBody("VALIDATION_ERROR", message, details));
    }

    private ResponseEntity<ErrorBody> reply(HttpStatus status, ErrorBody body) {
        return ResponseEntity.status(status).body(body);
    }
}

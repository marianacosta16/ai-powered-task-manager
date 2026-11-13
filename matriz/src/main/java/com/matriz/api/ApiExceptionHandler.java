package com.matriz.api;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.matriz.shared.BusinessRuleException;
import com.matriz.shared.ForbiddenException;
import com.matriz.shared.InvalidRequestException;
import com.matriz.shared.NotFoundException;

import jakarta.validation.ConstraintViolationException;

/** Errors of the contract: code + message, with the field when a value is invalid. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    static final String INVALID_CREDENTIALS =
            "Credentials are not correct. Check your username and password and try again.";

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorBody> invalidCredentials(InvalidCredentialsException ex) {
        return reply(HttpStatus.UNAUTHORIZED, ErrorBody.of("INVALID_CREDENTIALS", INVALID_CREDENTIALS));
    }

    @ExceptionHandler(RequestValidationException.class)
    ResponseEntity<ErrorBody> invalidBody(RequestValidationException ex) {
        return validation("Some values are missing or not valid. Check them and try again.", ex.details());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ErrorBody> invalidValue(InvalidRequestException ex) {
        return validation(ex.getMessage(), List.of(new ErrorBody.Detail(ex.field(), ex.issue())));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ErrorBody> businessRule(BusinessRuleException ex) {
        return reply(HttpStatus.UNPROCESSABLE_ENTITY, ErrorBody.of(ex.code(), ex.getMessage()));
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
                .map(e -> new ErrorBody.Detail(e.getField(), e.getDefaultMessage())).toList();
        return validation("Some values are missing or not valid. Check them and try again.", details);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class, MethodArgumentTypeMismatchException.class })
    ResponseEntity<ErrorBody> invalidRequest(Exception ex) {
        return validation("The request is not valid. Check the values and try again.", null);
    }

    /** Not in the contract's list of statuses, but the body keeps the same shape (code + message). */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorBody> unsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return reply(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorBody.of("VALIDATION_ERROR",
                "The request body must be JSON. Send it with Content-Type: application/json."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorBody> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (ex.getSupportedHttpMethods() != null) {
            response.allow(ex.getSupportedHttpMethods().toArray(new org.springframework.http.HttpMethod[0]));
        }
        return response.body(ErrorBody.of("VALIDATION_ERROR", "This operation does not accept that HTTP method."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorBody> unknownPath(NoResourceFoundException ex) {
        return reply(HttpStatus.NOT_FOUND, ErrorBody.of("NOT_FOUND", "The item was not found."));
    }

    private ResponseEntity<ErrorBody> validation(String message, List<ErrorBody.Detail> details) {
        return reply(HttpStatus.BAD_REQUEST, new ErrorBody("VALIDATION_ERROR", message, details));
    }

    private ResponseEntity<ErrorBody> reply(HttpStatus status, ErrorBody body) {
        return ResponseEntity.status(status).body(body);
    }
}

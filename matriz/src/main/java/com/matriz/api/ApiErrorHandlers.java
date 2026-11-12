package com.matriz.api;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes the 401 and 403 answers of the security filters in the same JSON format as the API. */
@Component
class ApiErrorHandlers {

    static final String SIGN_IN = "Sign in to continue.";
    static final String SESSION_ENDED = "Your session has ended. Sign in again to continue.";
    static final String FORBIDDEN = "You do not have access to this action.";

    private final ObjectMapper mapper;

    ApiErrorHandlers(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * No valid session. If the request carried a session cookie, that session ended (signed out
     * or 3 hours without activity), and the message says so (scenarios 10.5 and 10.6).
     */
    void unauthenticated(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        boolean sessionEnded = request.getRequestedSessionId() != null;
        write(response, HttpStatus.UNAUTHORIZED,
                ErrorBody.of("UNAUTHENTICATED", sessionEnded ? SESSION_ENDED : SIGN_IN));
    }

    void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, ErrorBody.of("FORBIDDEN", FORBIDDEN));
    }

    private void write(HttpServletResponse response, HttpStatus status, ErrorBody body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), body);
    }
}

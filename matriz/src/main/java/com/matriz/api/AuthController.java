package com.matriz.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matriz.access.AuthenticatedUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** signIn, signOut and getAuthenticatedUser (F10, US10). */
@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final String cookieName;

    AuthController(AuthenticationManager authenticationManager,
            @Value("${server.servlet.session.cookie.name:SESSION}") String cookieName) {
        this.authenticationManager = authenticationManager;
        this.cookieName = cookieName;
    }

    @PostMapping("/login")
    UserResponse signIn(@Valid @RequestBody SignInRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        // A wrong user or password raises an AuthenticationException: same 401 for both (scenario 10.2).
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));

        // A new session identifier at every sign in, against session fixation.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        return toResponse((AuthenticatedUser) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> signOut(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        // Ask the browser to forget the cookie, so a later request is "not signed in", not "session ended".
        ResponseCookie expired = ResponseCookie.from(cookieName, "").path("/").maxAge(0).httpOnly(true).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expired.toString()).build();
    }

    @GetMapping("/me")
    UserResponse getAuthenticatedUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return toResponse(user);
    }

    private static UserResponse toResponse(AuthenticatedUser user) {
        return new UserResponse(user.id(), user.name(), user.role().label());
    }
}

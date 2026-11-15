package com.matriz.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.matriz.testsupport.ApiIntegrationTest;

/**
 * CORS for the web interface (increment 9, P31): the allowed origin may call the API with credentials and read the
 * answers, also the errors and the total of a list; any other origin gets nothing. The test sends the {@code Origin}
 * header that a browser would send.
 */
class CorsIntegrationTest extends ApiIntegrationTest {

    private static final String ALLOWED = "http://localhost:5173";
    private static final String OTHER = "https://evil.example";

    @Test
    void theAllowedOriginPassesThePreflightOfAJsonRequestWithoutASession() {
        ResponseEntity<String> preflight = preflight(ALLOWED, "POST", "content-type");

        assertThat(preflight.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preflight.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED);
        assertThat(preflight.getHeaders().getAccessControlAllowCredentials()).isTrue();
        assertThat(preflight.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.PATCH, HttpMethod.DELETE);
        assertThat(preflight.getHeaders().getAccessControlAllowHeaders()).anyMatch(h -> h.equalsIgnoreCase("Content-Type"));
        assertThat(preflight.getHeaders().getAccessControlMaxAge()).isPositive();
    }

    @Test
    void anotherOriginIsRefusedTheCorsHeaders() {
        ResponseEntity<String> preflight = preflight(OTHER, "POST", "content-type");
        ResponseEntity<String> actual = withOrigin("/projects", OTHER, sessionOf("marta"));

        assertThat(preflight.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(preflight.getHeaders().getAccessControlAllowOrigin()).isNull();
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN); // "Invalid CORS request"
        assertThat(actual.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void aHeaderThatTheApiDoesNotUseIsNotAllowedInThePreflight() {
        ResponseEntity<String> onlyThat = preflight(ALLOWED, "POST", "x-custom");
        ResponseEntity<String> withOthers = preflight(ALLOWED, "POST", "content-type,x-custom");

        assertThat(onlyThat.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        // Spring answers with the headers it allows, and the browser then blocks the request that asked for more
        assertThat(withOthers.getHeaders().getAccessControlAllowHeaders()).doesNotContain("x-custom", "X-Custom");
    }

    @Test
    void theAllowedOriginReadsTheAnswersWithItsSessionAndTheTotalOfAList() {
        String marta = sessionOf("marta");

        ResponseEntity<String> list = withOrigin("/projects/" + projectId("Website") + "/tasks", ALLOWED, marta);

        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED);
        assertThat(list.getHeaders().getAccessControlAllowCredentials()).isTrue();
        assertThat(list.getHeaders().getAccessControlExposeHeaders()).contains("X-Total-Count"); // the browser lets the page read it
        assertThat(list.getHeaders().getFirst("X-Total-Count")).isNotNull();
    }

    @Test
    void theErrorsAreReadableByTheAllowedOriginToo() {
        ResponseEntity<String> unauthenticated = withOrigin("/projects", ALLOWED, null); // 401: the interface must read it
        ResponseEntity<String> forbidden = withOrigin("/projects/" + projectId("Website"), ALLOWED, sessionOf("sofia")); // 403

        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED);
        assertThat(unauthenticated.getBody()).contains("UNAUTHENTICATED");
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED);
    }

    @Test
    void signingInFromTheAllowedOriginAnswersWithCredentialsAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(ALLOWED);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        ResponseEntity<String> login = http.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"ana\",\"password\":\"" + password + "\"}", headers), String.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getHeaders().getAccessControlAllowOrigin()).isEqualTo(ALLOWED);
        assertThat(login.getHeaders().getAccessControlAllowCredentials()).isTrue();
        assertThat(login.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("SESSION=", "SameSite=Lax", "HttpOnly");
    }

    @Test
    void aWildcardOrASlashInTheAllowedOriginsStopsTheStart() {
        CorsConfig config = new CorsConfig();

        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("*"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("http://*.example.com"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(List.of("http://localhost:5173/"))).isInstanceOf(IllegalStateException.class);
        assertThat(config.corsConfigurationSource(List.of("http://localhost:5173", " http://localhost:4173 "))).isNotNull();
    }

    // ---- helpers ----

    private ResponseEntity<String> preflight(String origin, String method, String headers) {
        HttpHeaders request = new HttpHeaders();
        request.setOrigin(origin);
        request.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method);
        request.set(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, headers);
        return http.exchange("/projects", HttpMethod.OPTIONS, new HttpEntity<>(request), String.class);
    }

    private ResponseEntity<String> withOrigin(String path, String origin, String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin(origin);
        if (cookie != null) {
            headers.add(HttpHeaders.COOKIE, cookie);
        }
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }
}

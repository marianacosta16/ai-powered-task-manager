package com.matriz.api;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS for the web interface, which runs on another origin than the API (increment 9). Only the origins in
 * {@code matriz.cors.allowed-origins} may call the API from a browser, and with credentials, because the session is
 * a cookie (T3). The list is exact: a wildcard is refused at start, since it cannot go with credentials and would
 * open the API to any site. The answer exposes {@code X-Total-Count} so the interface can show the totals (A31).
 * The CSRF protection stays off (P31): the cookie is {@code SameSite=Lax}, the API only takes JSON (415 otherwise,
 * so a form from another site cannot reach it) and the origins are limited here.
 */
@Configuration
class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${matriz.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        List<String> origins = allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
        for (String origin : origins) {
            if (origin.contains("*") || origin.endsWith("/")) {
                throw new IllegalStateException("matriz.cors.allowed-origins must be exact origins like "
                        + "http://localhost:5173, without * and without a trailing slash: " + origin);
            }
        }
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "Accept"));
        cors.setExposedHeaders(List.of("X-Total-Count"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}

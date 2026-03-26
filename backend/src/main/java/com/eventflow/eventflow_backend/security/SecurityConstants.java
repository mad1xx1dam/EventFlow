package com.eventflow.eventflow_backend.security;

public class SecurityConstants {

    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/events/*/invite/*",
            "/api/v1/events/*/invite/*/rsvp",
            "/api/v1/events/*/invite/*/polls",
            "/api/v1/polls/*/vote",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/ws/**",
            "/error"
    };
}
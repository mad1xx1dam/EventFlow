package com.eventflow.eventflow_backend.controller;

import java.util.UUID;

import com.eventflow.eventflow_backend.dto.request.LoginRequest;
import com.eventflow.eventflow_backend.dto.request.RegisterRequest;
import com.eventflow.eventflow_backend.dto.request.ResendVerificationEmailRequest;
import com.eventflow.eventflow_backend.dto.response.AuthResponse;
import com.eventflow.eventflow_backend.dto.response.CurrentUserResponse;
import com.eventflow.eventflow_backend.dto.response.RegisterResponse;
import com.eventflow.eventflow_backend.dto.response.VerifyEmailResponse;
import com.eventflow.eventflow_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@RequestParam("token") UUID token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<RegisterResponse> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationEmailRequest request
    ) {
        return ResponseEntity.ok(authService.resendVerificationEmail(request));
    }
}
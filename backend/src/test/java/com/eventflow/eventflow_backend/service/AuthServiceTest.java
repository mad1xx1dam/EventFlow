package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.request.LoginRequest;
import com.eventflow.eventflow_backend.dto.request.RegisterRequest;
import com.eventflow.eventflow_backend.dto.response.AuthResponse;
import com.eventflow.eventflow_backend.dto.response.RegisterResponse;
import com.eventflow.eventflow_backend.dto.response.VerifyEmailResponse;
import com.eventflow.eventflow_backend.entity.EmailVerificationToken;
import com.eventflow.eventflow_backend.entity.Role;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.RoleCode;
import com.eventflow.eventflow_backend.mapper.AuthMapper;
import com.eventflow.eventflow_backend.repository.EmailVerificationTokenRepository;
import com.eventflow.eventflow_backend.repository.RoleRepository;
import com.eventflow.eventflow_backend.repository.UserRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import com.eventflow.eventflow_backend.security.JwtService;
import com.eventflow.eventflow_backend.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");
        request.setConfirmPassword("password");

        Role role = new Role();
        role.setCode(RoleCode.USER);

        User savedUser = new User();
        savedUser.setEmail("test@test.com");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID());

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(authMapper.toUser(any(), any(), anyString(), any())).thenReturn(new User());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authMapper.toEmailVerificationToken(any(), any(), any(), any())).thenReturn(token);
        when(authMapper.toRegisterResponse(anyString())).thenReturn(new RegisterResponse());

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(mailService).sendVerificationEmail(anyString(), any(UUID.class));
    }

    @Test
    void register_PasswordsDoNotMatch_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");
        request.setConfirmPassword("different");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void verifyEmail_Success() {
        UUID tokenUuid = UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setExpiresAt(OffsetDateTime.now().plusHours(1));

        User user = new User();
        token.setUser(user);

        when(tokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(token));
        when(authMapper.toVerifyEmailResponse(anyString())).thenReturn(new VerifyEmailResponse());

        VerifyEmailResponse response = authService.verifyEmail(tokenUuid);

        assertNotNull(response);
        assertTrue(user.getEmailVerified());
        assertTrue(user.getEnabled());
        assertNotNull(token.getUsedAt());
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_TokenExpired_ThrowsException() {
        UUID tokenUuid = UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(tokenRepository.findByToken(tokenUuid)).thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail(tokenUuid));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt_token");
        when(authMapper.toAuthResponse(userDetails, "jwt_token")).thenReturn(new AuthResponse());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        verify(jwtService).generateToken(userDetails);
    }
}
package com.eventflow.eventflow_backend.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.eventflow.eventflow_backend.dto.request.LoginRequest;
import com.eventflow.eventflow_backend.dto.request.RegisterRequest;
import com.eventflow.eventflow_backend.dto.request.ResendVerificationEmailRequest;
import com.eventflow.eventflow_backend.dto.response.AuthResponse;
import com.eventflow.eventflow_backend.dto.response.CurrentUserResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final AuthMapper authMapper;
    private final MailService mailService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Пароль и подтверждение пароля не совпадают");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        Role userRole = roleRepository.findByCode(RoleCode.USER)
                .orElseThrow(() -> new IllegalStateException("Роль USER не найдена"));

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusHours(24);

        User user = authMapper.toUser(request, userRole, encodedPassword, now);
        User savedUser = userRepository.save(user);

        EmailVerificationToken verificationToken = authMapper.toEmailVerificationToken(
                savedUser,
                UUID.randomUUID(),
                now,
                expiresAt
        );

        emailVerificationTokenRepository.save(verificationToken);

        mailService.sendVerificationEmail(savedUser.getEmail(), verificationToken.getToken());

        return authMapper.toRegisterResponse("Регистрация прошла успешно. Пожалуйста, подтвердите email.");
    }

    @Transactional
    public VerifyEmailResponse verifyEmail(UUID token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Неверный токен подтверждения"));

        if (verificationToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Токен подтверждения уже был использован");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (verificationToken.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Срок действия токена подтверждения истёк");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setUpdatedAt(now);

        verificationToken.setUsedAt(now);

        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);

        return authMapper.toVerifyEmailResponse("Email успешно подтверждён");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException ex) {
            throw new DisabledException("Аккаунт не активирован");
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Неверный email или пароль");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails);

        return authMapper.toAuthResponse(userDetails, accessToken);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        User user = currentUserService.getCurrentUserOrThrow();
        return authMapper.toCurrentUserResponse(user);
    }

    @Transactional
    public RegisterResponse resendVerificationEmail(ResendVerificationEmailRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Не раскрываем, существует пользователь или нет
        if (user == null) {
            return authMapper.toRegisterResponse(
                    "Если аккаунт с таким email существует, письмо с подтверждением будет отправлено повторно."
            );
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return authMapper.toRegisterResponse("Email уже подтверждён.");
        }

        List<EmailVerificationToken> activeTokens =
                emailVerificationTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId());

        OffsetDateTime now = OffsetDateTime.now();

        for (EmailVerificationToken activeToken : activeTokens) {
            activeToken.setUsedAt(now);
        }

        emailVerificationTokenRepository.saveAll(activeTokens);

        EmailVerificationToken verificationToken = authMapper.toEmailVerificationToken(
                user,
                UUID.randomUUID(),
                now,
                now.plusHours(24)
        );

        emailVerificationTokenRepository.save(verificationToken);

        mailService.sendVerificationEmail(user.getEmail(), verificationToken.getToken());

        return authMapper.toRegisterResponse(
                "Если аккаунт с таким email существует, письмо с подтверждением будет отправлено повторно."
        );
    }
}
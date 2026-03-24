package com.eventflow.eventflow_backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.eventflow.eventflow_backend.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(UUID token);

    List<EmailVerificationToken> findAllByUserIdAndUsedAtIsNull(Long userId);
}
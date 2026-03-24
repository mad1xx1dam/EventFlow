package com.eventflow.eventflow_backend.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.eventflow.eventflow_backend.dto.request.RegisterRequest;
import com.eventflow.eventflow_backend.dto.response.AuthResponse;
import com.eventflow.eventflow_backend.dto.response.CurrentUserResponse;
import com.eventflow.eventflow_backend.dto.response.RegisterResponse;
import com.eventflow.eventflow_backend.dto.response.VerifyEmailResponse;
import com.eventflow.eventflow_backend.entity.EmailVerificationToken;
import com.eventflow.eventflow_backend.entity.Role;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.security.UserDetailsImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(request.getName().trim())")
    @Mapping(target = "email", expression = "java(request.getEmail().trim().toLowerCase())")
    @Mapping(target = "passwordHash", source = "encodedPassword")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "enabled", constant = "false")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    User toUser(RegisterRequest request, Role role, String encodedPassword, OffsetDateTime now);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "token", source = "token")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "usedAt", ignore = true)
    @Mapping(target = "createdAt", source = "now")
    EmailVerificationToken toEmailVerificationToken(User user, UUID token, OffsetDateTime now, OffsetDateTime expiresAt);

    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "userId", source = "userDetails.id")
    @Mapping(target = "email", source = "userDetails.email")
    @Mapping(target = "role", expression = "java(userDetails.getRoleCode().name())")
    AuthResponse toAuthResponse(UserDetailsImpl userDetails, String accessToken);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "role", expression = "java(user.getRole().getCode().name())")
    @Mapping(target = "emailVerified", source = "emailVerified")
    @Mapping(target = "enabled", source = "enabled")
    CurrentUserResponse toCurrentUserResponse(User user);

    @Mapping(target = "message", source = "message")
    RegisterResponse toRegisterResponse(String message);

    @Mapping(target = "message", source = "message")
    VerifyEmailResponse toVerifyEmailResponse(String message);
}
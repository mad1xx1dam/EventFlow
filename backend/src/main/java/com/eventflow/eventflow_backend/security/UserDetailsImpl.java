package com.eventflow.eventflow_backend.security;

import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.RoleCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Boolean enabled;
    private final Boolean emailVerified;
    private final RoleCode roleCode;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(
            Long id,
            String email,
            String password,
            Boolean enabled,
            Boolean emailVerified,
            RoleCode roleCode,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.roleCode = roleCode;
        this.authorities = authorities;
    }

    public static UserDetailsImpl fromUser(User user) {
        RoleCode roleCode = user.getRole().getCode();

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getEnabled(),
                user.getEmailVerified(),
                roleCode,
                List.of(new SimpleGrantedAuthority("ROLE_" + roleCode.name()))
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled) && Boolean.TRUE.equals(emailVerified);
    }
}
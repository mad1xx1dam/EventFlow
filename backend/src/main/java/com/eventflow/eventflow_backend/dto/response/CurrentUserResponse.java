package com.eventflow.eventflow_backend.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CurrentUserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Boolean emailVerified;
    private Boolean enabled;
}
package com.eventflow.eventflow_backend.repository;

import com.eventflow.eventflow_backend.entity.Role;
import com.eventflow.eventflow_backend.entity.enums.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(RoleCode code);
}

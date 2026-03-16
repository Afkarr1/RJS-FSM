package com.rjs.fsm.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // ===== AUTH LOOKUP =====
    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    // ===== USER EXISTENCE CHECK =====
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    // ===== SAFE FETCH =====
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    // ===== LIST USERS PER TENANT =====
    List<User> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
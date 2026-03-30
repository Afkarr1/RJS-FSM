package com.rjs.fsm.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndTenantId(String username, UUID tenantId);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    List<User> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<User> findByTenantIdAndRoleOrderByFullNameAsc(UUID tenantId, UserRole role);

    List<User> findByTenantIdAndRoleAndTechSectionOrderByFullNameAsc(
        UUID tenantId, UserRole role, TechSection techSection);
}

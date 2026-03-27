package com.rjs.fsm.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserLoginAuditRepository extends JpaRepository<UserLoginAudit, UUID> {

    List<UserLoginAudit> findByTenantIdOrderByLoggedInAtDesc(UUID tenantId);

    List<UserLoginAudit> findByUserIdOrderByLoggedInAtDesc(UUID userId);

    List<UserLoginAudit> findAllByOrderByLoggedInAtDesc();
}

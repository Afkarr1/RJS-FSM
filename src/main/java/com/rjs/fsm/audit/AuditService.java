package com.rjs.fsm.audit;

import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditRepo;
    private final UserLoginAuditRepository loginRepo;

    public AuditService(AuditLogRepository auditRepo, UserLoginAuditRepository loginRepo) {
        this.auditRepo = auditRepo;
        this.loginRepo = loginRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID actorId, String action, String entityType, UUID entityId, String detail) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            log.warn("Cannot log audit: tenant context not set");
            return;
        }

        AuditLog entry = new AuditLog();
        entry.setTenantId(tenantId);
        entry.setActorUserId(actorId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetail("{\"message\":\"" + escapeJson(detail) + "\"}");

        auditRepo.save(entry);
        log.debug("Audit: {} {} on {} {}", actorId, action, entityType, entityId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogin(User user, String ipAddress, String userAgent) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) return;

        UserLoginAudit entry = new UserLoginAudit();
        entry.setTenantId(tenantId);
        entry.setUserId(user.getId());
        entry.setUsername(user.getUsername());
        entry.setRole(user.getRole().name());
        entry.setIpAddress(ipAddress);
        entry.setUserAgent(userAgent);

        loginRepo.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs() {
        UUID tenantId = TenantContext.require();
        return auditRepo.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<UserLoginAudit> getLoginAudit() {
        UUID tenantId = TenantContext.require();
        return loginRepo.findByTenantIdOrderByLoggedInAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<UserLoginAudit> getLoginAuditByUser(UUID userId) {
        return loginRepo.findByUserIdOrderByLoggedInAtDesc(userId);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r");
    }
}

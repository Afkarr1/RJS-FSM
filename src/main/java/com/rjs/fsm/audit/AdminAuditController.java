package com.rjs.fsm.audit;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    public List<AuditLog> getAuditLogs() {
        return auditService.getAuditLogs();
    }

    @GetMapping("/logins")
    public List<UserLoginAudit> getLoginAudit() {
        return auditService.getLoginAudit();
    }

    @GetMapping("/logins/{userId}")
    public List<UserLoginAudit> getUserLoginAudit(@PathVariable UUID userId) {
        return auditService.getLoginAuditByUser(userId);
    }
}

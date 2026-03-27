package com.rjs.fsm.dev;

import com.rjs.fsm.audit.AuditLog;
import com.rjs.fsm.audit.AuditLogRepository;
import com.rjs.fsm.audit.UserLoginAudit;
import com.rjs.fsm.audit.UserLoginAuditRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/dev")
public class DevLogController {

    private static final String DEV_PASSWORD = "$$$$$$$";

    private final AuditLogRepository auditLogRepo;
    private final UserLoginAuditRepository loginAuditRepo;

    public DevLogController(AuditLogRepository auditLogRepo, UserLoginAuditRepository loginAuditRepo) {
        this.auditLogRepo = auditLogRepo;
        this.loginAuditRepo = loginAuditRepo;
    }

    @GetMapping("/logs")
    public List<AuditLog> getActivityLogs(
            @RequestHeader(value = "X-Dev-Password", required = false) String devPassword,
            HttpServletResponse response) throws IOException {
        if (!DEV_PASSWORD.equals(devPassword)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Akses ditolak. Password developer salah.\"}");
            return null;
        }
        return auditLogRepo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/logins")
    public List<UserLoginAudit> getLoginLogs(
            @RequestHeader(value = "X-Dev-Password", required = false) String devPassword,
            HttpServletResponse response) throws IOException {
        if (!DEV_PASSWORD.equals(devPassword)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Akses ditolak. Password developer salah.\"}");
            return null;
        }
        return loginAuditRepo.findAllByOrderByLoggedInAtDesc();
    }
}

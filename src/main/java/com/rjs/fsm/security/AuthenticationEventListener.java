package com.rjs.fsm.security;

import com.rjs.fsm.audit.AuditService;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import com.rjs.fsm.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class AuthenticationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final UserRepository userRepo;
    private final UserService userService;
    private final AuditService auditService;

    public AuthenticationEventListener(UserRepository userRepo, UserService userService,
                                        AuditService auditService) {
        this.userRepo = userRepo;
        this.userService = userService;
        this.auditService = auditService;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        UUID tenantId = TenantContext.get();
        if (tenantId == null) return;

        userRepo.findByUsernameAndTenantId(username, tenantId).ifPresent(user -> {
            userService.updateLastLogin(user.getId());

            String ip = null;
            String userAgent = null;
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                var req = sra.getRequest();
                ip = req.getHeader("X-Forwarded-For");
                if (ip == null) ip = req.getRemoteAddr();
                userAgent = req.getHeader("User-Agent");
            }

            auditService.logLogin(user, ip, userAgent);
            log.info("Login success: user={}, tenant={}", username, tenantId);
        });
    }
}

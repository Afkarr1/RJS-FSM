package com.rjs.fsm.security;

import com.rjs.fsm.exception.ForbiddenException;
import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepo;

    public CurrentUserProvider(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ForbiddenException("Not authenticated");
        }
        UUID tenantId = TenantContext.require();
        return userRepo.findByUsernameAndTenantId(auth.getName(), tenantId)
                .orElseThrow(() -> new ForbiddenException("User not found in current tenant"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}

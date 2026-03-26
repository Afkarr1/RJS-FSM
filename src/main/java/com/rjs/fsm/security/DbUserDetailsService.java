package com.rjs.fsm.security;

import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public DbUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new UsernameNotFoundException("Tenant context not set");
        }

        User user = userRepo.findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isActive(),   // enabled
                true,              // accountNonExpired
                true,              // credentialsNonExpired
                true,              // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}

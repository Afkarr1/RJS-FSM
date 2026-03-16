package com.rjs.fsm.security;

import com.rjs.fsm.tenant.TenantContext;
import com.rjs.fsm.user.User;
import com.rjs.fsm.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.utill.UUID;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DbUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        // Ambil tenant dari TenantContext
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new UsernameNotFoundException("Tenant context not set");
        }
        
        // Lookup user berdasarkan username + tenant
        User u = userRepository
                .findByUsernameAndTenantId(username, tenantId)
                .orElseThrow(() -> 
                        new UsernameNotFoundException(  
                                "User not found: " + username + " in tenant " + tenantId
                                )
                            );

        // Build authority
        String role = "ROLE_" + u.getRole().name();

        // Return spring security user
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .authorities(new SimpleGrantedAuthority(role))
                .disabled(!u.isActive()) // kalau inactive => login gagal (401)
                .build();
    }
}

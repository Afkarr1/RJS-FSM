package com.rjs.fsm.security;

import com.rjs.fsm.tenant.TenantFilter;
import com.rjs.fsm.tenant.TenantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // TenantFilter bean
    @Bean
    public TenantFilter tenantFilter(TenantRepository tenantRepository) {
        return new TenantFilter(tenantRepository);
    }

    // Security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TenantFilter tenantFilter) throws Exception {
      
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/reviews/**").permitAll()

                //ADMIN endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                //TECH endpoints
                .requestMatchers("/api/tech/**").hasRole("TECHNICIAN")

                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

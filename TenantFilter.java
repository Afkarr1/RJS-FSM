package com.rjs.fsm.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepo;

    public TenantFilter(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String raw = request.getHeader("X-Tenant-Id");
        if (raw == null || raw.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"BAD_REQUEST\",\"message\":\"Missing X-Tenant-Id\"}");
            return;
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"BAD_REQUEST\",\"message\":\"Invalid X-Tenant-Id\"}");
            return;
        }

        // validate tenant exists
        if (!tenantRepo.existsById(tenantId)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"BAD_REQUEST\",\"message\":\"Unknown tenant\"}");
            return;
        }

        try {
            TenantContext.set(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
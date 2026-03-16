package com.rjs.fsm.tenant;

import com.rjs.fsm.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Single-tenant mode: auto-resolves to default tenant.
 * If X-Tenant-Id header is provided, it validates and uses it (future multi-tenant support).
 * Public endpoints (/api/health, /api/public/**) skip tenant resolution.
 */
@Component
public class TenantFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepo;
    private final UUID defaultTenantId;

    public TenantFilter(TenantRepository tenantRepo, AppProperties props) {
        this.tenantRepo = tenantRepo;
        this.defaultTenantId = UUID.fromString(props.getTenant().getDefaultId());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/health") || path.startsWith("/api/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String raw = request.getHeader("X-Tenant-Id");
        UUID tenantId;

        if (raw != null && !raw.isBlank()) {
            try {
                tenantId = UUID.fromString(raw.trim());
            } catch (IllegalArgumentException e) {
                sendError(response, "Invalid X-Tenant-Id format");
                return;
            }
            if (!tenantRepo.existsById(tenantId)) {
                sendError(response, "Unknown tenant");
                return;
            }
        } else {
            tenantId = defaultTenantId;
        }

        try {
            TenantContext.set(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"BAD_REQUEST\",\"message\":\"" + message + "\"}");
    }
}

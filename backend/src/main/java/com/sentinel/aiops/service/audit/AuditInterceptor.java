package com.sentinel.aiops.service.audit;

import com.sentinel.aiops.domain.AuditEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Records every state-changing API call to the audit log. Runs as an MVC interceptor so
 * the authenticated principal and final HTTP status are both available, and writes
 * asynchronously so auditing never adds latency to the request.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditService audit;

    public AuditInterceptor(AuditService audit) { this.audit = audit; }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        if (!MUTATING.contains(req.getMethod())) return;
        String uri = req.getRequestURI();
        if (uri.startsWith("/api/auth/login")) return; // don't log credentials traffic

        String actor = "anonymous", role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            actor = auth.getName();
            role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse(null);
        }
        audit.record(AuditEvent.builder()
                .actor(actor).role(role)
                .action(req.getMethod() + " " + uri)
                .target(uri)
                .status(res.getStatus())
                .ip(clientIp(req))
                .detail(ex == null ? null : ex.getClass().getSimpleName() + ": " + ex.getMessage())
                .build());
    }

    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}

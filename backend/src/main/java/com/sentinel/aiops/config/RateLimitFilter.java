package com.sentinel.aiops.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client token-bucket rate limiter — protects the platform from abusive or runaway
 * clients at scale. In-memory (per instance); for multi-instance deployments back this
 * with Redis (see roadmap). Returns HTTP 429 with a Retry-After hint when exhausted.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final long capacity;
    private final double refillPerSec;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${aiops.ratelimit.enabled:true}") boolean enabled,
            @Value("${aiops.ratelimit.capacity:120}") long capacity,
            @Value("${aiops.ratelimit.refill-per-minute:120}") long refillPerMinute) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSec = refillPerMinute / 60.0;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        // Don't throttle health/metrics/docs or the live SSE stream.
        return !enabled || p.startsWith("/actuator") || p.startsWith("/swagger")
                || p.startsWith("/v3/api-docs") || p.startsWith("/api/stream");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String key = clientKey(req);
        if (buckets.size() > 50_000) buckets.clear(); // crude safety valve for unbounded growth
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        if (bucket.tryConsume(refillPerSec, capacity)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setHeader("Retry-After", "1");
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\"}");
        }
    }

    private String clientKey(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }

    /** Lazily-refilled token bucket. */
    private static final class Bucket {
        private double tokens;
        private long lastNanos = System.nanoTime();
        Bucket(long initial) { this.tokens = initial; }

        synchronized boolean tryConsume(double refillPerSec, long capacity) {
            long now = System.nanoTime();
            double elapsedSec = (now - lastNanos) / 1_000_000_000.0;
            lastNanos = now;
            tokens = Math.min(capacity, tokens + elapsedSec * refillPerSec);
            if (tokens >= 1.0) { tokens -= 1.0; return true; }
            return false;
        }
    }
}

package com.farmatodo.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration BUCKET_TTL = Duration.ofMinutes(15);

    private final int requestsPerMinute;
    private final Cache<String, Bucket> bucketCache;

    public RateLimitFilter(@Value("${app.rate-limit.requests-per-minute:60}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        this.bucketCache = Caffeine.newBuilder()
                .expireAfterAccess(BUCKET_TTL)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return !path.startsWith("/tokens") && !path.startsWith("/clients")
                && !path.startsWith("/products") && !path.startsWith("/carts")
                && !path.startsWith("/orders");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = resolveClientKey(request);
        Bucket bucket = bucketCache.get(key, k -> createBucket());

        if (!bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Too many requests. Rate limit exceeded.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

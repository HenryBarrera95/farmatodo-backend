package com.farmatodo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(3)
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.api.key}")
    private String apiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return !path.startsWith("/tokens") && !path.startsWith("/clients")
                && !path.startsWith("/products") && !path.startsWith("/carts")
                && !path.startsWith("/orders");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("X-API-KEY");
        if (header == null || !header.equals(apiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing API Key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

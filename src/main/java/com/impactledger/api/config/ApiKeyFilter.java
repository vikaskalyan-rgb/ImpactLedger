package com.impactledger.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A single-user tool doesn't need full Spring Security / JWT — just a shared
 * secret header so a random visitor to your public Render URL can't read or
 * write your data. Set APP_API_KEY as an env var and send it as X-API-KEY
 * from the frontend.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.api-key}")
    private String apiKey;

    private static final String HEADER_NAME = "X-API-KEY";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // allow CORS preflight and a public health check through
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER_NAME);
        if (apiKey == null || apiKey.isBlank() || apiKey.equals(providedKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing or invalid X-API-KEY header\"}");
        }
    }
}
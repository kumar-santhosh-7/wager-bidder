package com.ledgerbid.api.config;

import com.ledgerbid.api.entity.UserAccount;
import com.ledgerbid.api.error.ApiException;
import com.ledgerbid.api.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthFilter extends OncePerRequestFilter {
    private final AuthService auth;

    public AuthFilter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri.endsWith("/api/health")
                || uri.endsWith("/api/auth/login")
                || uri.endsWith("/health")
                || uri.endsWith("/auth/login")
                || uri.contains("/api/files/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        try {
            UserAccount user = auth.requireUser(token);
            request.setAttribute("authUser", user);
            filterChain.doFilter(request, response);
        } catch (ApiException ex) {
            response.setStatus(ex.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"ok\":false,\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}");
        }
    }
}

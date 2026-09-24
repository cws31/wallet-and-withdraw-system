package com.veloop.rewards.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecurityExceptionHandler securityExceptionHandler;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            SecurityExceptionHandler securityExceptionHandler) {
        this.jwtService = jwtService;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authHeader.startsWith("Bearer ")) {

            securityExceptionHandler.sendUnauthorized(
                    request,
                    response,
                    "Invalid Authorization header");

            return;
        }

        String token = authHeader.substring(7).trim();

        if (token.isBlank()) {

            securityExceptionHandler.sendUnauthorized(
                    request,
                    response,
                    "Bearer token is missing");

            return;
        }

        try {

            if (!jwtService.isTokenValid(token)) {

                securityExceptionHandler.sendUnauthorized(
                        request,
                        response,
                        "Invalid or expired token");

                return;
            }

            Long userId = jwtService.extractUserId(token);

            String role = jwtService
                    .extractAllClaims(token)
                    .get("role", String.class);

            if (userId == null || role == null || role.isBlank()) {

                securityExceptionHandler.sendUnauthorized(
                        request,
                        response,
                        "Invalid token claims");

                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role)));

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException ex) {

            SecurityContextHolder.clearContext();

            securityExceptionHandler.sendUnauthorized(
                    request,
                    response,
                    "Invalid or expired token");
        }
    }
}
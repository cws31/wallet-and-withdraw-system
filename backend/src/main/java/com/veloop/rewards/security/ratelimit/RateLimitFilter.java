package com.veloop.rewards.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private static final String WITHDRAWAL_PATH = "/api/withdrawals";
    private static final String WALLET_PATH = "/api/wallet";

    private final RateLimitService rateLimitService;
    private final RateLimitProperties properties;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            RateLimitProperties properties) {

        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        /*
         * Authentication endpoints are public,
         * therefore they are rate-limited by IP address.
         */
        if (isLoginOrRegister(path)) {

            RateLimitDecision decision = rateLimitService.check(
                    "ip:" + getClientIp(request),
                    properties.getLoginRequests(),
                    properties.getLoginWindowSeconds());

            if (!decision.allowed()) {
                sendTooManyRequests(response, decision);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            filterChain.doFilter(request, response);
            return;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Long userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isWithdrawalCreation(method, path)) {

            RateLimitDecision decision = rateLimitService.check(
                    "withdrawal:" + userId,
                    properties.getWithdrawalRequests(),
                    properties.getWithdrawalWindowSeconds());

            if (!decision.allowed()) {
                sendTooManyRequests(response, decision);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        if (isWalletMutation(method, path)) {

            RateLimitDecision decision = rateLimitService.check(
                    "wallet:" + userId,
                    properties.getWalletMutationRequests(),
                    properties.getWalletMutationWindowSeconds());

            if (!decision.allowed()) {
                sendTooManyRequests(response, decision);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        if (isWithdrawalMutation(method, path)) {

            RateLimitDecision decision = rateLimitService.check(
                    "withdrawal-mutation:" + userId,
                    properties.getWithdrawalMutationRequests(),
                    properties.getWithdrawalMutationWindowSeconds());

            if (!decision.allowed()) {
                sendTooManyRequests(response, decision);
                return;
            }

            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginOrRegister(String path) {

        return LOGIN_PATH.equals(path)
                || REGISTER_PATH.equals(path);
    }

    private boolean isWithdrawalCreation(
            String method,
            String path) {

        return "POST".equalsIgnoreCase(method)
                && WITHDRAWAL_PATH.equals(path);
    }

    private boolean isWalletMutation(
            String method,
            String path) {

        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }

        return (WALLET_PATH + "/credit").equals(path)
                || (WALLET_PATH + "/debit").equals(path);
    }

    private boolean isWithdrawalMutation(
            String method,
            String path) {

        if (!"PATCH".equalsIgnoreCase(method)) {
            return false;
        }

        if (!path.startsWith(WITHDRAWAL_PATH + "/")) {
            return false;
        }

        return path.endsWith("/processing")
                || path.endsWith("/approve")
                || path.endsWith("/reject")
                || path.endsWith("/cancel");
    }

    private String getClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(
            HttpServletResponse response,
            RateLimitDecision decision)
            throws IOException {

        response.setStatus(
                HttpStatus.TOO_MANY_REQUESTS.value());

        response.setHeader(
                "Retry-After",
                String.valueOf(
                        decision.retryAfterSeconds()));

        response.setContentType(
                "application/json");

        response.getWriter().write(
                """
                        {
                          "status": 429,
                          "error": "Too Many Requests",
                          "message": "Too many requests. Please try again later."
                        }
                        """);
    }
}
package com.veloop.rewards.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
            CorrelationIdFilter.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final int MAX_CORRELATION_ID_LENGTH = 100;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);
        String requestId = UUID.randomUUID().toString();

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        response.setHeader(
                CORRELATION_ID_HEADER,
                correlationId);

        response.setHeader(
                REQUEST_ID_HEADER,
                requestId);

        Instant startedAt = Instant.now();

        try {

            filterChain.doFilter(
                    request,
                    response);

        } finally {

            long durationMs = Duration.between(
                    startedAt,
                    Instant.now()).toMillis();

            log.atInfo()
                    .addKeyValue(
                            "event",
                            "request.completed")
                    .addKeyValue(
                            "method",
                            request.getMethod())
                    .addKeyValue(
                            "path",
                            request.getRequestURI())
                    .addKeyValue(
                            "status",
                            response.getStatus())
                    .addKeyValue(
                            "durationMs",
                            durationMs)
                    .log("HTTP request completed");

            MDC.remove(
                    CORRELATION_ID_MDC_KEY);

            MDC.remove(
                    REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveCorrelationId(
            HttpServletRequest request) {

        String suppliedCorrelationId = request.getHeader(
                CORRELATION_ID_HEADER);

        if (suppliedCorrelationId == null
                || suppliedCorrelationId.isBlank()
                || suppliedCorrelationId.length() > MAX_CORRELATION_ID_LENGTH) {

            return UUID.randomUUID().toString();
        }

        return suppliedCorrelationId.trim();
    }
}
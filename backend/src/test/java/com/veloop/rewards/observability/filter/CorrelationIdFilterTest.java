package com.veloop.rewards.observability.filter;

import com.veloop.rewards.observability.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPreserveExistingCorrelationIdAndGenerateRequestId()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn("correlation-123");

        when(request.getHeader("X-Request-ID"))
                .thenReturn("request-123");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/test");

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(response)
                .setHeader(
                        "X-Correlation-ID",
                        "correlation-123");

        verify(response)
                .setHeader(
                        eq("X-Request-ID"),
                        argThat(this::isValidUuid));

        verify(filterChain)
                .doFilter(request, response);

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateMissingCorrelationAndRequestIds()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn(null);

        when(request.getHeader("X-Request-ID"))
                .thenReturn(null);

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/test");

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(response)
                .setHeader(
                        eq("X-Correlation-ID"),
                        argThat(this::isValidUuid));

        verify(response)
                .setHeader(
                        eq("X-Request-ID"),
                        argThat(this::isValidUuid));

        verify(filterChain)
                .doFilter(request, response);

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldUseCorrelationIdAndGeneratedRequestIdInsideFilterChain()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn("correlation-test");

        when(request.getHeader("X-Request-ID"))
                .thenReturn("request-test");

        when(request.getMethod())
                .thenReturn("POST");

        when(request.getRequestURI())
                .thenReturn("/api/wallet");

        doAnswer(invocation -> {

            assertEquals(
                    "correlation-test",
                    MDC.get("correlationId"));

            assertNotNull(
                    MDC.get("requestId"));

            assertTrue(
                    isValidUuid(
                            MDC.get("requestId")));

            assertNotEquals(
                    "request-test",
                    MDC.get("requestId"));

            return null;

        }).when(filterChain)
                .doFilter(request, response);

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(filterChain)
                .doFilter(request, response);

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateNewCorrelationIdWhenExistingIdIsBlank()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn("   ");

        when(request.getHeader("X-Request-ID"))
                .thenReturn("request-existing");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/test");

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(response)
                .setHeader(
                        eq("X-Correlation-ID"),
                        argThat(this::isValidUuid));

        verify(response)
                .setHeader(
                        eq("X-Request-ID"),
                        argThat(this::isValidUuid));

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateNewRequestIdWhenExistingIdIsBlank()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn("correlation-existing");

        when(request.getHeader("X-Request-ID"))
                .thenReturn("   ");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/test");

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(response)
                .setHeader(
                        "X-Correlation-ID",
                        "correlation-existing");

        verify(response)
                .setHeader(
                        eq("X-Request-ID"),
                        argThat(this::isValidUuid));

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldClearMdcWhenFilterChainThrowsException()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn("correlation-error");

        when(request.getHeader("X-Request-ID"))
                .thenReturn("request-error");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/error");

        doThrow(new RuntimeException("test error"))
                .when(filterChain)
                .doFilter(request, response);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> filter.doFilter(
                        request,
                        response,
                        filterChain));

        assertEquals(
                "test error",
                exception.getMessage());

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateNewCorrelationIdWhenExistingIdIsOversized()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        String oversizedCorrelationId = "a".repeat(300);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn(oversizedCorrelationId);

        when(request.getHeader("X-Request-ID"))
                .thenReturn("request-valid");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/test");

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(response)
                .setHeader(
                        eq("X-Correlation-ID"),
                        argThat(value -> value != null
                                && !value.isBlank()
                                && !value.equals(
                                        oversizedCorrelationId)
                                && isValidUuid(value)));

        verify(response)
                .setHeader(
                        eq("X-Request-ID"),
                        argThat(this::isValidUuid));

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void shouldGenerateNewRequestIdWhenExistingIdIsOversized()
            throws Exception {

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("X-Correlation-ID"))
                .thenReturn("correlation-valid");

        String oversizedRequestId = "b".repeat(300);

        when(request.getHeader("X-Request-ID"))
                .thenReturn(oversizedRequestId);

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/test");

        filter.doFilter(
                request,
                response,
                filterChain);

        verify(response)
                .setHeader(
                        "X-Correlation-ID",
                        "correlation-valid");

        verify(response)
                .setHeader(
                        eq("X-Request-ID"),
                        argThat(value -> value != null
                                && !value.isBlank()
                                && !value.equals(
                                        oversizedRequestId)
                                && isValidUuid(value)));

        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestId"));
    }

    private boolean isValidUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
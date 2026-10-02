package com.veloop.rewards.security.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitProperties properties;
    private RateLimitService rateLimitService;
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        properties = new RateLimitProperties();

        properties.setEnabled(true);

        properties.setLoginRequests(2);
        properties.setLoginWindowSeconds(60);

        properties.setWithdrawalRequests(2);
        properties.setWithdrawalWindowSeconds(60);

        properties.setWalletMutationRequests(2);
        properties.setWalletMutationWindowSeconds(60);

        properties.setWithdrawalMutationRequests(2);
        properties.setWithdrawalMutationWindowSeconds(60);

        properties.setMaxEntries(10_000);

        rateLimitService = new RateLimitService(properties);

        rateLimitFilter = new RateLimitFilter(
                rateLimitService,
                properties);
    }

    @Test
    void shouldAllowLoginRequestsWithinLimit() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        rateLimitFilter.doFilter(
                request,
                response,
                filterChain);

        verify(filterChain).doFilter(request, response);

        assertEquals(
                200,
                response.getStatus());
    }

    @Test
    void shouldRejectLoginRequestWhenLimitIsExceeded()
            throws Exception {

        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {

            MockHttpServletRequest request = new MockHttpServletRequest();

            request.setMethod("POST");
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("127.0.0.1");

            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilter(
                    request,
                    response,
                    filterChain);
        }

        MockHttpServletRequest thirdRequest = new MockHttpServletRequest();

        thirdRequest.setMethod("POST");
        thirdRequest.setRequestURI("/api/auth/login");
        thirdRequest.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                thirdRequest,
                thirdResponse,
                filterChain);

        assertEquals(
                429,
                thirdResponse.getStatus());

        assertNotNull(
                thirdResponse.getHeader("Retry-After"));

        verify(filterChain, times(2))
                .doFilter(any(), any());
    }

    @Test
    void shouldUseDifferentRateLimitBucketsForDifferentIps()
            throws Exception {

        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest firstIpRequest = new MockHttpServletRequest();

        firstIpRequest.setMethod("POST");
        firstIpRequest.setRequestURI("/api/auth/login");
        firstIpRequest.setRemoteAddr("192.168.1.10");

        MockHttpServletResponse firstIpResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                firstIpRequest,
                firstIpResponse,
                filterChain);

        MockHttpServletRequest secondIpRequest = new MockHttpServletRequest();

        secondIpRequest.setMethod("POST");
        secondIpRequest.setRequestURI("/api/auth/login");
        secondIpRequest.setRemoteAddr("192.168.1.20");

        MockHttpServletResponse secondIpResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                secondIpRequest,
                secondIpResponse,
                filterChain);

        assertEquals(
                200,
                firstIpResponse.getStatus());

        assertEquals(
                200,
                secondIpResponse.getStatus());
    }

    @Test
    void shouldRateLimitAuthenticatedWithdrawalCreation()
            throws Exception {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        100L,
                        null,
                        List.of()));

        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {

            MockHttpServletRequest request = new MockHttpServletRequest();

            request.setMethod("POST");
            request.setRequestURI("/api/withdrawals");

            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilter(
                    request,
                    response,
                    filterChain);
        }

        MockHttpServletRequest thirdRequest = new MockHttpServletRequest();

        thirdRequest.setMethod("POST");
        thirdRequest.setRequestURI("/api/withdrawals");

        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

        rateLimitFilter.doFilter(
                thirdRequest,
                thirdResponse,
                filterChain);

        assertEquals(
                429,
                thirdResponse.getStatus());

        assertNotNull(
                thirdResponse.getHeader("Retry-After"));
    }

    @Test
    void shouldNotRateLimitUnauthenticatedProtectedRequest()
            throws Exception {

        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("GET");
        request.setRequestURI("/api/wallet");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        rateLimitFilter.doFilter(
                request,
                response,
                filterChain);

        verify(filterChain).doFilter(
                request,
                response);

        assertEquals(
                200,
                response.getStatus());
    }

    @Test
    void shouldAllowRequestsWhenRateLimitingIsDisabled()
            throws Exception {

        properties.setEnabled(false);

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {

            rateLimitFilter.doFilter(
                    request,
                    response,
                    filterChain);
        }

        verify(
                filterChain,
                times(10)).doFilter(request, response);

        assertEquals(
                200,
                response.getStatus());
    }
}
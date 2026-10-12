package com.veloop.rewards.security.ratelimit;

import com.veloop.rewards.observability.ObservabilityMetrics;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

        private RateLimitProperties properties;
        private RateLimitService rateLimitService;
        private RateLimitFilter rateLimitFilter;
        private StringRedisTemplate redisTemplate;
        private ObservabilityMetrics observabilityMetrics;

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

                redisTemplate = mock(StringRedisTemplate.class);
                observabilityMetrics = mock(ObservabilityMetrics.class);

                rateLimitService = new RateLimitService(
                                properties,
                                redisTemplate,
                                observabilityMetrics);

                rateLimitFilter = new RateLimitFilter(
                                rateLimitService,
                                properties);
        }

        @Test
        void shouldAllowLoginRequestsWithinLimit()
                        throws Exception {

                when(redisTemplate.execute(
                                any(),
                                anyList(),
                                any()))
                                .thenReturn("1:60");

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

                verify(filterChain)
                                .doFilter(request, response);

                assertEquals(
                                200,
                                response.getStatus());

                verifyNoInteractions(observabilityMetrics);
        }

        @Test
        void shouldRejectLoginRequestWhenLimitIsExceeded()
                        throws Exception {

                when(redisTemplate.execute(
                                any(),
                                anyList(),
                                any()))
                                .thenReturn(
                                                "1:60",
                                                "2:59",
                                                "3:58");

                FilterChain filterChain = mock(FilterChain.class);

                for (int i = 0; i < 2; i++) {

                        MockHttpServletRequest request = new MockHttpServletRequest();

                        request.setMethod("POST");
                        request.setRequestURI(
                                        "/api/auth/login");
                        request.setRemoteAddr(
                                        "127.0.0.1");

                        MockHttpServletResponse response = new MockHttpServletResponse();

                        rateLimitFilter.doFilter(
                                        request,
                                        response,
                                        filterChain);

                        assertEquals(
                                        200,
                                        response.getStatus());
                }

                MockHttpServletRequest thirdRequest = new MockHttpServletRequest();

                thirdRequest.setMethod("POST");
                thirdRequest.setRequestURI(
                                "/api/auth/login");
                thirdRequest.setRemoteAddr(
                                "127.0.0.1");

                MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

                rateLimitFilter.doFilter(
                                thirdRequest,
                                thirdResponse,
                                filterChain);

                assertEquals(
                                429,
                                thirdResponse.getStatus());

                assertNotNull(
                                thirdResponse.getHeader(
                                                "Retry-After"));

                verify(
                                filterChain,
                                times(2))
                                .doFilter(any(), any());

                verify(observabilityMetrics)
                                .recordRateLimitBlocked();

                verifyNoMoreInteractions(
                                observabilityMetrics);
        }

        @Test
        void shouldUseDifferentRateLimitBucketsForDifferentIps()
                        throws Exception {

                when(redisTemplate.execute(
                                any(),
                                anyList(),
                                any()))
                                .thenReturn("1:60");

                FilterChain filterChain = mock(FilterChain.class);

                MockHttpServletRequest firstIpRequest = new MockHttpServletRequest();

                firstIpRequest.setMethod("POST");
                firstIpRequest.setRequestURI(
                                "/api/auth/login");
                firstIpRequest.setRemoteAddr(
                                "192.168.1.10");

                MockHttpServletResponse firstIpResponse = new MockHttpServletResponse();

                rateLimitFilter.doFilter(
                                firstIpRequest,
                                firstIpResponse,
                                filterChain);

                MockHttpServletRequest secondIpRequest = new MockHttpServletRequest();

                secondIpRequest.setMethod("POST");
                secondIpRequest.setRequestURI(
                                "/api/auth/login");
                secondIpRequest.setRemoteAddr(
                                "192.168.1.20");

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

                verify(
                                redisTemplate,
                                times(2))
                                .execute(
                                                any(),
                                                anyList(),
                                                eq("60"));

                verifyNoInteractions(
                                observabilityMetrics);
        }

        @Test
        void shouldRateLimitAuthenticatedWithdrawalCreation()
                        throws Exception {

                when(redisTemplate.execute(
                                any(),
                                anyList(),
                                any()))
                                .thenReturn(
                                                "1:60",
                                                "2:59",
                                                "3:58");

                SecurityContextHolder
                                .getContext()
                                .setAuthentication(
                                                new UsernamePasswordAuthenticationToken(
                                                                100L,
                                                                null,
                                                                List.of()));

                FilterChain filterChain = mock(FilterChain.class);

                for (int i = 0; i < 2; i++) {

                        MockHttpServletRequest request = new MockHttpServletRequest();

                        request.setMethod("POST");
                        request.setRequestURI(
                                        "/api/withdrawals");

                        MockHttpServletResponse response = new MockHttpServletResponse();

                        rateLimitFilter.doFilter(
                                        request,
                                        response,
                                        filterChain);

                        assertEquals(
                                        200,
                                        response.getStatus());
                }

                MockHttpServletRequest thirdRequest = new MockHttpServletRequest();

                thirdRequest.setMethod("POST");
                thirdRequest.setRequestURI(
                                "/api/withdrawals");

                MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

                rateLimitFilter.doFilter(
                                thirdRequest,
                                thirdResponse,
                                filterChain);

                assertEquals(
                                429,
                                thirdResponse.getStatus());

                assertNotNull(
                                thirdResponse.getHeader(
                                                "Retry-After"));

                verify(observabilityMetrics)
                                .recordRateLimitBlocked();

                verifyNoMoreInteractions(
                                observabilityMetrics);
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

                verify(filterChain)
                                .doFilter(
                                                request,
                                                response);

                assertEquals(
                                200,
                                response.getStatus());

                verifyNoInteractions(redisTemplate);

                verifyNoInteractions(observabilityMetrics);
        }

        @Test
        void shouldAllowRequestsWhenRateLimitingIsDisabled()
                        throws Exception {

                properties.setEnabled(false);

                MockHttpServletRequest request = new MockHttpServletRequest();

                request.setMethod("POST");
                request.setRequestURI(
                                "/api/auth/login");
                request.setRemoteAddr(
                                "127.0.0.1");

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
                                times(10))
                                .doFilter(
                                                request,
                                                response);

                assertEquals(
                                200,
                                response.getStatus());

                verifyNoInteractions(redisTemplate);

                verifyNoInteractions(observabilityMetrics);
        }

        @Test
        void shouldUseForwardedClientIp()
                        throws Exception {

                when(redisTemplate.execute(
                                any(),
                                anyList(),
                                any()))
                                .thenReturn("1:60");

                MockHttpServletRequest request = new MockHttpServletRequest();

                request.setMethod("POST");
                request.setRequestURI(
                                "/api/auth/login");

                request.setRemoteAddr(
                                "192.168.1.10");

                request.addHeader(
                                "X-Forwarded-For",
                                "10.0.0.1, 192.168.1.20");

                MockHttpServletResponse response = new MockHttpServletResponse();

                FilterChain filterChain = mock(FilterChain.class);

                rateLimitFilter.doFilter(
                                request,
                                response,
                                filterChain);

                assertEquals(
                                200,
                                response.getStatus());

                verify(redisTemplate)
                                .execute(
                                                any(),
                                                anyList(),
                                                eq("60"));

                verifyNoInteractions(
                                observabilityMetrics);
        }

        @Test
        void shouldFailOpenWhenRedisIsUnavailable()
                        throws Exception {

                when(redisTemplate.execute(
                                any(),
                                anyList(),
                                any()))
                                .thenThrow(
                                                new RuntimeException(
                                                                "Redis unavailable"));

                MockHttpServletRequest request = new MockHttpServletRequest();

                request.setMethod("POST");
                request.setRequestURI(
                                "/api/auth/login");

                request.setRemoteAddr(
                                "127.0.0.1");

                MockHttpServletResponse response = new MockHttpServletResponse();

                FilterChain filterChain = mock(FilterChain.class);

                rateLimitFilter.doFilter(
                                request,
                                response,
                                filterChain);

                assertEquals(
                                200,
                                response.getStatus());

                verify(filterChain)
                                .doFilter(
                                                request,
                                                response);

                verifyNoInteractions(
                                observabilityMetrics);
        }
}

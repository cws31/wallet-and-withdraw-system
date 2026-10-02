package com.veloop.rewards.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitProperties properties;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {

        properties = new RateLimitProperties();

        properties.setEnabled(true);
        properties.setMaxEntries(10_000);

        rateLimitService = new RateLimitService(
                properties,
                redisTemplate);
    }

    @Test
    void shouldAllowRequestWithinLimit() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn("1:60");

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                5,
                60);

        assertTrue(decision.allowed());
        assertEquals(
                0,
                decision.retryAfterSeconds());

        verify(redisTemplate, times(1))
                .execute(
                        any(),
                        anyList(),
                        eq("60"));
    }

    @Test
    void shouldAllowMultipleRequestsWithinLimit() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn("1:60")
                .thenReturn("2:59")
                .thenReturn("3:58");

        assertTrue(
                rateLimitService.check(
                        "user:1",
                        3,
                        60).allowed());

        assertTrue(
                rateLimitService.check(
                        "user:1",
                        3,
                        60).allowed());

        assertTrue(
                rateLimitService.check(
                        "user:1",
                        3,
                        60).allowed());

        verify(redisTemplate, times(3))
                .execute(
                        any(),
                        anyList(),
                        eq("60"));
    }

    @Test
    void shouldRejectRequestWhenLimitExceeded() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn("6:42");

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                5,
                60);

        assertFalse(decision.allowed());

        assertEquals(
                42,
                decision.retryAfterSeconds());

        verify(redisTemplate, times(1))
                .execute(
                        any(),
                        anyList(),
                        eq("60"));
    }

    @Test
    void shouldUseSeparateBucketsForDifferentUsers() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn("1:60");

        RateLimitDecision userOne = rateLimitService.check(
                "user:1",
                5,
                60);

        RateLimitDecision userTwo = rateLimitService.check(
                "user:2",
                5,
                60);

        assertTrue(userOne.allowed());
        assertTrue(userTwo.allowed());

        verify(redisTemplate, times(2))
                .execute(
                        any(),
                        anyList(),
                        eq("60"));
    }

    @Test
    void shouldPermitWhenRateLimitingIsDisabled() {

        properties.setEnabled(false);

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                1,
                60);

        assertTrue(decision.allowed());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldPermitWhenKeyIsNull() {

        RateLimitDecision decision = rateLimitService.check(
                null,
                5,
                60);

        assertTrue(decision.allowed());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldPermitWhenKeyIsBlank() {

        RateLimitDecision decision = rateLimitService.check(
                "   ",
                5,
                60);

        assertTrue(decision.allowed());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldPermitWhenConfigurationIsInvalid() {

        RateLimitDecision zeroLimit = rateLimitService.check(
                "user:1",
                0,
                60);

        RateLimitDecision negativeLimit = rateLimitService.check(
                "user:2",
                -1,
                60);

        RateLimitDecision zeroWindow = rateLimitService.check(
                "user:3",
                5,
                0);

        RateLimitDecision negativeWindow = rateLimitService.check(
                "user:4",
                5,
                -10);

        assertTrue(zeroLimit.allowed());
        assertTrue(negativeLimit.allowed());
        assertTrue(zeroWindow.allowed());
        assertTrue(negativeWindow.allowed());

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void shouldReturnMinimumRetryAfterWhenRedisTtlIsZero() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn("6:0");

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                5,
                60);

        assertFalse(decision.allowed());

        assertEquals(
                1,
                decision.retryAfterSeconds());
    }

    @Test
    void shouldPermitWhenRedisReturnsNull() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn(null);

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                5,
                60);

        assertTrue(decision.allowed());

        verify(redisTemplate, times(1))
                .execute(
                        any(),
                        anyList(),
                        eq("60"));
    }

    @Test
    void shouldPermitWhenRedisReturnsInvalidResult() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenReturn("invalid-result");

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                5,
                60);

        assertTrue(decision.allowed());
    }

    @Test
    void shouldFailOpenWhenRedisIsUnavailable() {

        when(redisTemplate.execute(
                any(),
                anyList(),
                any()))
                .thenThrow(
                        new RuntimeException(
                                "Redis connection failed"));

        RateLimitDecision decision = rateLimitService.check(
                "user:1",
                5,
                60);

        assertTrue(decision.allowed());

        verify(redisTemplate, times(1))
                .execute(
                        any(),
                        anyList(),
                        eq("60"));
    }
}
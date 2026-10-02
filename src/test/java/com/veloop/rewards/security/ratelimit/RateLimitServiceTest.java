package com.veloop.rewards.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();

        properties.setEnabled(true);
        properties.setMaxEntries(10_000);

        rateLimitService = new RateLimitService(properties);
    }

    @Test
    void shouldAllowRequestsWithinLimit() {

        RateLimitDecision first = rateLimitService.check("user-1", 5, 60);

        RateLimitDecision second = rateLimitService.check("user-1", 5, 60);

        RateLimitDecision third = rateLimitService.check("user-1", 5, 60);

        assertTrue(first.allowed());
        assertTrue(second.allowed());
        assertTrue(third.allowed());

        assertEquals(0, first.retryAfterSeconds());
    }

    @Test
    void shouldRejectRequestWhenLimitIsExceeded() {

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = rateLimitService.check("user-1", 5, 60);

            assertTrue(decision.allowed());
        }

        RateLimitDecision sixthRequest = rateLimitService.check("user-1", 5, 60);

        assertFalse(sixthRequest.allowed());
        assertTrue(sixthRequest.retryAfterSeconds() > 0);
    }

    @Test
    void shouldKeepLimitsSeparateForDifferentUsers() {

        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = rateLimitService.check("user-1", 5, 60);

            assertTrue(decision.allowed());
        }

        RateLimitDecision userOne = rateLimitService.check("user-1", 5, 60);

        RateLimitDecision userTwo = rateLimitService.check("user-2", 5, 60);

        assertFalse(userOne.allowed());
        assertTrue(userTwo.allowed());
    }

    @Test
    void shouldAllowRequestsWhenRateLimitingIsDisabled() {

        RateLimitProperties properties = new RateLimitProperties();

        properties.setEnabled(false);

        RateLimitService service = new RateLimitService(properties);

        for (int i = 0; i < 100; i++) {

            RateLimitDecision decision = service.check("user-1", 5, 60);

            assertTrue(decision.allowed());
        }
    }

    @Test
    void shouldNotRateLimitBlankKeys() {

        RateLimitDecision blank = rateLimitService.check("", 5, 60);

        RateLimitDecision nullKey = rateLimitService.check(null, 5, 60);

        assertTrue(blank.allowed());
        assertTrue(nullKey.allowed());
    }

    @Test
    void shouldNotRateLimitInvalidConfiguration() {

        RateLimitDecision zeroLimit = rateLimitService.check("user-1", 0, 60);

        RateLimitDecision zeroWindow = rateLimitService.check("user-2", 5, 0);

        assertTrue(zeroLimit.allowed());
        assertTrue(zeroWindow.allowed());
    }

    @Test
    void shouldCreateSeparateWindowForDifferentKeys() {

        RateLimitDecision userOne = rateLimitService.check("user-1", 1, 60);

        RateLimitDecision userTwo = rateLimitService.check("user-2", 1, 60);

        assertTrue(userOne.allowed());
        assertTrue(userTwo.allowed());

        RateLimitDecision userOneSecond = rateLimitService.check("user-1", 1, 60);

        assertFalse(userOneSecond.allowed());
    }

    @Test
    void shouldReturnAtLeastOneSecondForRetryAfter() {

        for (int i = 0; i < 5; i++) {
            rateLimitService.check("user-1", 5, 60);
        }

        RateLimitDecision decision = rateLimitService.check("user-1", 5, 60);

        assertFalse(decision.allowed());
        assertTrue(decision.retryAfterSeconds() >= 1);
    }
}
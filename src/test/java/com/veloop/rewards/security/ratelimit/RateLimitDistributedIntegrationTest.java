package com.veloop.rewards.security.ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitDistributedIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private RateLimitProperties properties;

    @BeforeAll
    static void startRedisConnection() {

        connectionFactory = new LettuceConnectionFactory(
                "localhost",
                6379);

        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(
                connectionFactory);

        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void closeRedisConnection() {

        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {

        properties = new RateLimitProperties();

        properties.setEnabled(true);
        properties.setMaxEntries(10_000);
    }

    @Test
    void shouldShareRateLimitStateAcrossServiceInstances() {

        String key = "distributed-test-" +
                UUID.randomUUID();

        RateLimitService instanceOne = new RateLimitService(
                properties,
                redisTemplate);

        RateLimitService instanceTwo = new RateLimitService(
                properties,
                redisTemplate);

        try {

            RateLimitDecision firstRequest = instanceOne.check(
                    key,
                    2,
                    60);

            RateLimitDecision secondRequest = instanceTwo.check(
                    key,
                    2,
                    60);

            RateLimitDecision thirdRequest = instanceOne.check(
                    key,
                    2,
                    60);

            assertTrue(
                    firstRequest.allowed());

            assertTrue(
                    secondRequest.allowed());

            assertFalse(
                    thirdRequest.allowed());

            assertTrue(
                    thirdRequest.retryAfterSeconds() > 0);

        } finally {

            redisTemplate.delete(
                    "veloop:ratelimit:" + key);
        }
    }

    @Test
    void shouldUseSeparateRedisCountersForDifferentUsers() {

        String userOneKey = "user-one-" +
                UUID.randomUUID();

        String userTwoKey = "user-two-" +
                UUID.randomUUID();

        RateLimitService instanceOne = new RateLimitService(
                properties,
                redisTemplate);

        RateLimitService instanceTwo = new RateLimitService(
                properties,
                redisTemplate);

        try {

            for (int i = 0; i < 2; i++) {

                RateLimitDecision decision = instanceOne.check(
                        userOneKey,
                        2,
                        60);

                assertTrue(
                        decision.allowed());
            }

            RateLimitDecision userOneThirdRequest = instanceTwo.check(
                    userOneKey,
                    2,
                    60);

            RateLimitDecision userTwoFirstRequest = instanceTwo.check(
                    userTwoKey,
                    2,
                    60);

            assertFalse(
                    userOneThirdRequest.allowed());

            assertTrue(
                    userTwoFirstRequest.allowed());

        } finally {

            redisTemplate.delete(
                    "veloop:ratelimit:" + userOneKey);

            redisTemplate.delete(
                    "veloop:ratelimit:" + userTwoKey);
        }
    }

    @Test
    void shouldCreateRedisCounterWithExpiry() {

        String key = "expiry-test-" +
                UUID.randomUUID();

        RateLimitService rateLimitService = new RateLimitService(
                properties,
                redisTemplate);

        String redisKey = "veloop:ratelimit:" + key;

        try {

            RateLimitDecision decision = rateLimitService.check(
                    key,
                    5,
                    60);

            assertTrue(
                    decision.allowed());

            String storedValue = redisTemplate.opsForValue()
                    .get(redisKey);

            assertEquals(
                    "1",
                    storedValue);

            Long ttl = redisTemplate.getExpire(
                    redisKey);

            assertNotNull(ttl);

            assertTrue(
                    ttl > 0);

            assertTrue(
                    ttl <= 60);

        } finally {

            redisTemplate.delete(
                    redisKey);
        }
    }

    @Test
    void shouldReturnRetryAfterFromRedisTtl() {

        String key = "retry-test-" +
                UUID.randomUUID();

        RateLimitService rateLimitService = new RateLimitService(
                properties,
                redisTemplate);

        String redisKey = "veloop:ratelimit:" + key;

        try {

            rateLimitService.check(
                    key,
                    1,
                    60);

            RateLimitDecision rejected = rateLimitService.check(
                    key,
                    1,
                    60);

            assertFalse(
                    rejected.allowed());

            assertTrue(
                    rejected.retryAfterSeconds() >= 1);

            Long redisTtl = redisTemplate.getExpire(
                    redisKey);

            assertNotNull(redisTtl);

            assertTrue(
                    redisTtl >= 1);

            assertTrue(
                    rejected.retryAfterSeconds() <= redisTtl + 1);

        } finally {

            redisTemplate.delete(
                    redisKey);
        }
    }
}
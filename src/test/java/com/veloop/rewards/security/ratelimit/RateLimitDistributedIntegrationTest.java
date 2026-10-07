package com.veloop.rewards.security.ratelimit;

import com.veloop.rewards.observability.ObservabilityMetrics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitDistributedIntegrationTest {

        private static LettuceConnectionFactory connectionFactory;
        private static StringRedisTemplate redisTemplate;

        private RateLimitProperties properties;
        private ObservabilityMetrics observabilityMetrics;

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

                observabilityMetrics = mock(ObservabilityMetrics.class);
        }

        @Test
        void shouldShareRateLimitStateAcrossServiceInstances() {

                String key = "distributed-test-" +
                                UUID.randomUUID();

                RateLimitService instanceOne = new RateLimitService(
                                properties,
                                redisTemplate,
                                observabilityMetrics);

                RateLimitService instanceTwo = new RateLimitService(
                                properties,
                                redisTemplate,
                                observabilityMetrics);

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

                        verify(observabilityMetrics)
                                        .recordRateLimitBlocked();

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
                                redisTemplate,
                                observabilityMetrics);

                RateLimitService instanceTwo = new RateLimitService(
                                properties,
                                redisTemplate,
                                observabilityMetrics);

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

                        verify(observabilityMetrics)
                                        .recordRateLimitBlocked();

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
                                redisTemplate,
                                observabilityMetrics);

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

                        verifyNoInteractions(
                                        observabilityMetrics);

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
                                redisTemplate,
                                observabilityMetrics);

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

                        verify(observabilityMetrics)
                                        .recordRateLimitBlocked();

                } finally {

                        redisTemplate.delete(
                                        redisKey);
                }
        }
}

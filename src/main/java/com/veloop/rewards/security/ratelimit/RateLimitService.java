package com.veloop.rewards.security.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateLimitService {

    private static final String KEY_PREFIX = "veloop:ratelimit:";
    private static final DefaultRedisScript<String> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
                    local count = redis.call('INCR', KEYS[1])

                    if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end

                    local ttl = redis.call('TTL', KEYS[1])

                    return tostring(count) .. ':' .. tostring(ttl)
                    """,
            String.class);

    private final RateLimitProperties properties;

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(
            RateLimitProperties properties,
            StringRedisTemplate redisTemplate) {

        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public RateLimitDecision check(
            String key,
            int maxRequests,
            int windowSeconds) {

        if (!properties.isEnabled()) {
            return RateLimitDecision.permit();
        }

        if (key == null || key.isBlank()) {
            return RateLimitDecision.permit();
        }

        if (maxRequests <= 0 || windowSeconds <= 0) {
            return RateLimitDecision.permit();
        }

        String redisKey = KEY_PREFIX + key;

        try {

            String result = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(redisKey),
                    String.valueOf(windowSeconds));

            if (result == null || result.isBlank()) {
                return RateLimitDecision.permit();
            }

            String[] parts = result.split(":", 2);

            if (parts.length != 2) {
                return RateLimitDecision.permit();
            }

            long requestCount = Long.parseLong(parts[0]);

            long ttl = Long.parseLong(parts[1]);

            if (requestCount <= maxRequests) {
                return RateLimitDecision.permit();
            }

            long retryAfterSeconds = normalizeRetryAfter(ttl);

            return RateLimitDecision.reject(
                    retryAfterSeconds);

        } catch (Exception exception) {

            return RateLimitDecision.permit();
        }
    }

    private long normalizeRetryAfter(
            long ttl) {

        if (ttl <= 0) {
            return 1;
        }

        return Math.max(1, ttl);
    }
}
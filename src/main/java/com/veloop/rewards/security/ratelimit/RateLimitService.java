package com.veloop.rewards.security.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final RateLimitProperties properties;

    private final Map<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public synchronized RateLimitDecision check(
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

        cleanupExpiredEntries(windowSeconds);

        Instant now = Instant.now();

        RequestWindow existing = requestWindows.get(key);

        /*
         * No existing window means this is the first request.
         */
        if (existing == null) {

            requestWindows.put(
                    key,
                    new RequestWindow(
                            now,
                            1,
                            windowSeconds));

            return RateLimitDecision.permit();
        }

        /*
         * If the current window has expired,
         * start a completely new window.
         */
        if (hasExpired(existing, now)) {

            requestWindows.put(
                    key,
                    new RequestWindow(
                            now,
                            1,
                            windowSeconds));

            return RateLimitDecision.permit();
        }

        /*
         * The current window is still active and
         * the maximum number of requests has already
         * been consumed.
         */
        if (existing.requestCount() >= maxRequests) {

            return RateLimitDecision.reject(
                    secondsUntilWindowExpires(
                            existing,
                            now));
        }

        /*
         * The request is within the allowed limit.
         * Increase the counter by one.
         */
        RequestWindow updatedWindow = new RequestWindow(
                existing.windowStart(),
                existing.requestCount() + 1,
                existing.windowSeconds());

        requestWindows.put(key, updatedWindow);

        return RateLimitDecision.permit();
    }

    private boolean hasExpired(
            RequestWindow window,
            Instant now) {
        return !now.isBefore(
                window.windowStart()
                        .plusSeconds(window.windowSeconds()));
    }

    private long secondsUntilWindowExpires(
            RequestWindow window,
            Instant now) {
        Instant expiresAt = window.windowStart()
                .plusSeconds(window.windowSeconds());

        long seconds = Duration.between(now, expiresAt)
                .getSeconds();

        return Math.max(1, seconds);
    }

    private void cleanupExpiredEntries(int currentWindowSeconds) {

        int maxEntries = properties.getMaxEntries();

        if (requestWindows.size() <= maxEntries) {
            return;
        }

        Instant now = Instant.now();

        Iterator<Map.Entry<String, RequestWindow>> iterator = requestWindows.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, RequestWindow> entry = iterator.next();

            RequestWindow window = entry.getValue();

            if (hasExpired(window, now)) {
                iterator.remove();
            }

            if (requestWindows.size() <= maxEntries) {
                break;
            }
        }
    }

    private record RequestWindow(
            Instant windowStart,
            int requestCount,
            int windowSeconds) {
    }
}
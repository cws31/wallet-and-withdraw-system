package com.veloop.rewards.security.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        long retryAfterSeconds) {

    public static RateLimitDecision allowed() {
        return new RateLimitDecision(true, 0);
    }

    public static RateLimitDecision rejected(long retryAfterSeconds) {
        return new RateLimitDecision(
                false,
                Math.max(1, retryAfterSeconds));
    }
}
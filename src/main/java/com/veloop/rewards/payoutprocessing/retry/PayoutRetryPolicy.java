package com.veloop.rewards.payoutprocessing.retry;

import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PayoutRetryPolicy {

    private static final int MAX_ATTEMPTS = 3;

    private static final long INITIAL_DELAY_SECONDS = 60L;

    public boolean canRetry(int attemptCount) {
        return attemptCount < MAX_ATTEMPTS;
    }

    public Duration getDelay(int attemptCount) {

        if (attemptCount <= 0) {
            return Duration.ofSeconds(
                    INITIAL_DELAY_SECONDS);
        }

        long multiplier = 1L << (attemptCount - 1);

        return Duration.ofSeconds(
                INITIAL_DELAY_SECONDS * multiplier);
    }

    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }
}
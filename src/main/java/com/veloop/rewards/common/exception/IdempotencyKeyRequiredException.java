package com.veloop.rewards.common.exception;

public class IdempotencyKeyRequiredException extends BusinessException {

    public IdempotencyKeyRequiredException() {
        super("Idempotency-Key header is required");
    }
}
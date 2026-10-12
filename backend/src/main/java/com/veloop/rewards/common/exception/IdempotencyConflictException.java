package com.veloop.rewards.common.exception;

public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException() {
        super("The Idempotency-Key has already been used for a different withdrawal request");
    }
}
package com.veloop.rewards.common.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(
            String currency,
            BigDecimal available,
            BigDecimal requested) {

        super(
                "Insufficient " + currency
                        + " balance. Available: " + available
                        + ", requested: " + requested);
    }
}
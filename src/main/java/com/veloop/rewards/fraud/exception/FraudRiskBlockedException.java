package com.veloop.rewards.fraud.exception;

import com.veloop.rewards.common.exception.BusinessException;

public class FraudRiskBlockedException extends BusinessException {

    public FraudRiskBlockedException(String message) {
        super(message);
    }
}

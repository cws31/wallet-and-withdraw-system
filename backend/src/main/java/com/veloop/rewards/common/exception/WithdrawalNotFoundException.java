package com.veloop.rewards.common.exception;

public class WithdrawalNotFoundException extends BusinessException {

    public WithdrawalNotFoundException(String withdrawalId) {
        super("Withdrawal not found: " + withdrawalId);
    }
}
package com.veloop.rewards.common.exception;

public class WithdrawalConcurrencyException extends BusinessException {

    public WithdrawalConcurrencyException() {
        super("Withdrawal could not be completed because the wallet was modified by another request. Please retry.");
    }
}
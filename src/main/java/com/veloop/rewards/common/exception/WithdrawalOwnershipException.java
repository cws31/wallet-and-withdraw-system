package com.veloop.rewards.common.exception;

public class WithdrawalOwnershipException extends BusinessException {

    public WithdrawalOwnershipException() {
        super("Withdrawal does not belong to the authenticated user");
    }
}
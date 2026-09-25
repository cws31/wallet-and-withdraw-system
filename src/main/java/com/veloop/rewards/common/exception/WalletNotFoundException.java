package com.veloop.rewards.common.exception;

public class WalletNotFoundException extends BusinessException {

    public WalletNotFoundException(Long userId) {
        super("Wallet not found for user: " + userId);
    }
}
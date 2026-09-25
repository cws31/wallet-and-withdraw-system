package com.veloop.rewards.common.exception;

public class WalletAlreadyExistsException extends BusinessException {

    public WalletAlreadyExistsException(Long userId) {
        super("Wallet already exists for user: " + userId);
    }
}
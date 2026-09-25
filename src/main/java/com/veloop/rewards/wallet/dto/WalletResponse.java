package com.veloop.rewards.wallet.dto;

import com.veloop.rewards.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        BigDecimal ves,
        BigDecimal sves,
        BigDecimal gems,
        BigDecimal tokens,
        BigDecimal spins,
        BigDecimal withdrawnVes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getVes(),
                wallet.getSves(),
                wallet.getGems(),
                wallet.getTokens(),
                wallet.getSpins(),
                wallet.getWithdrawnVes(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt());
    }
}
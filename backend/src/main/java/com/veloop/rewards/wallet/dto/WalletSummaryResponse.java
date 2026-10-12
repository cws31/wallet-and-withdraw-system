package com.veloop.rewards.wallet.dto;

import java.math.BigDecimal;

public record WalletSummaryResponse(
        BigDecimal ves,
        BigDecimal sves,
        BigDecimal gems,
        BigDecimal tokens,
        BigDecimal spins,
        long totalTransactions) {
}
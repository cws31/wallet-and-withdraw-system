package com.veloop.rewards.reconciliation.dto;

import com.veloop.rewards.wallet.enums.Currency;

import java.math.BigDecimal;

public record ReconciliationResult(
        Long walletId,
        Long userId,
        Currency currency,
        BigDecimal walletBalance,
        BigDecimal ledgerDerivedBalance,
        BigDecimal difference,
        boolean reconciled) {
}
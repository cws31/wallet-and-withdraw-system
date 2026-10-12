package com.veloop.rewards.wallet.dto;

import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionResponse(
        String transactionId,
        Currency currency,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String source,
        String referenceId,
        TransactionStatus status,
        String description,
        LocalDateTime createdAt) {

    public static WalletTransactionResponse from(
            WalletTransaction transaction) {

        return new WalletTransactionResponse(
                transaction.getTransactionId(),
                transaction.getCurrency(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getSource(),
                transaction.getReferenceId(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }
}
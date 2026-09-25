package com.veloop.rewards.wallet.dto;

import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WalletCreditRequest(

        @NotNull Currency currency,

        @NotNull @DecimalMin(value = "0.0001", message = "Amount must be greater than zero") BigDecimal amount,

        @NotNull TransactionType transactionType,

        String source,

        String referenceId,

        String description,

        String metadata) {
}
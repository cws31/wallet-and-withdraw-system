package com.veloop.rewards.payout.dto;

import com.veloop.rewards.payout.entity.PayoutOption;

import java.math.BigDecimal;

public record PayoutOptionResponse(
        Long id,
        BigDecimal payoutAmount,
        String currency,
        BigDecimal currencyAmount) {

    public static PayoutOptionResponse from(PayoutOption option) {
        return new PayoutOptionResponse(
                option.getId(),
                option.getPayoutAmount(),
                option.getCurrency(),
                option.getCurrencyAmount());
    }
}

package com.veloop.rewards.payoutprocessing.provider;

import java.math.BigDecimal;

public record PayoutProviderRequest(

        String withdrawalId,

        String payoutMethodCode,

        String currency,

        BigDecimal currencyAmount,

        BigDecimal payoutAmount,

        String payoutDetails

) {
}
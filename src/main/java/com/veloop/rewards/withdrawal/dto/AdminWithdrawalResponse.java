
package com.veloop.rewards.withdrawal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminWithdrawalResponse(
        Long userId,
        String userName,
        String userEmail,
        String withdrawalId,
        Long payoutMethodId,
        String payoutMethod,
        Long payoutOptionId,
        String currency,
        BigDecimal currencyAmount,
        BigDecimal payoutAmount,
        String payoutDetails,
        String status,
        String rejectionReason,
        String reviewNote,
        Long transactionId,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

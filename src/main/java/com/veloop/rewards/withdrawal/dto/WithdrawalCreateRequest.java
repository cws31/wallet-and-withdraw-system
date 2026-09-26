package com.veloop.rewards.withdrawal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class WithdrawalCreateRequest {

    @NotNull(message = "Payout method ID is required")
    @Positive(message = "Payout method ID must be positive")
    private Long payoutMethodId;

    @NotNull(message = "Payout option ID is required")
    @Positive(message = "Payout option ID must be positive")
    private Long payoutOptionId;

    @NotBlank(message = "Payout details are required")
    private String payoutDetails;

    public Long getPayoutMethodId() {
        return payoutMethodId;
    }

    public void setPayoutMethodId(Long payoutMethodId) {
        this.payoutMethodId = payoutMethodId;
    }

    public Long getPayoutOptionId() {
        return payoutOptionId;
    }

    public void setPayoutOptionId(Long payoutOptionId) {
        this.payoutOptionId = payoutOptionId;
    }

    public String getPayoutDetails() {
        return payoutDetails;
    }

    public void setPayoutDetails(String payoutDetails) {
        this.payoutDetails = payoutDetails;
    }
}
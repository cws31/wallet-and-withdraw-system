package com.veloop.rewards.payout.dto;

import com.veloop.rewards.payout.entity.PayoutMethod;

import java.util.List;

public record PayoutMethodResponse(
        Long id,
        String code,
        String name,
        List<PayoutOptionResponse> options) {

    public static PayoutMethodResponse from(
            PayoutMethod method,
            List<PayoutOptionResponse> options) {
        return new PayoutMethodResponse(
                method.getId(),
                method.getCode(),
                method.getName(),
                options);
    }
}

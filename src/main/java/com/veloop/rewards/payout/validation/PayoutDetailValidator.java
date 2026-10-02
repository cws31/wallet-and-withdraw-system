package com.veloop.rewards.payout.validation;

import org.springframework.stereotype.Component;

@Component
public class PayoutDetailValidator {

    private static final String UPI = "UPI";

    public void validate(
            String payoutMethodCode,
            String payoutDetails) {

        if (payoutMethodCode == null
                || payoutMethodCode.isBlank()) {

            throw new IllegalArgumentException(
                    "Payout method is required");
        }

        if (payoutDetails == null
                || payoutDetails.isBlank()) {

            throw new IllegalArgumentException(
                    "Payout details are required");
        }

        String methodCode = payoutMethodCode.trim().toUpperCase();

        String details = payoutDetails.trim();

        switch (methodCode) {

            case UPI -> validateUpi(details);

            default -> {

            }
        }
    }

    private void validateUpi(String payoutDetails) {
        if (!payoutDetails.matches(
                "^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$")) {

            throw new IllegalArgumentException(
                    "Invalid UPI ID");
        }
    }
}

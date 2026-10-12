package com.veloop.rewards.payout.validation;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import org.springframework.stereotype.Component;

@Component
public class PayoutDetailValidator {

    private static final String UPI = "UPI";
    private static final String AMAZON_GIFT_CARD = "AMAZON_GIFT_CARD";
    private static final String GOOGLE_PLAY_GIFT_CARD = "GOOGLE_PLAY_GIFT_CARD";

    private static final String UPI_PATTERN = "^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$";

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]{1,64}"
            + "@"
            + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
            + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$";

    public void validate(
            String payoutMethodCode,
            String payoutDetails) {

        if (payoutMethodCode == null
                || payoutMethodCode.isBlank()) {

            throw new InvalidWithdrawalRequestException(
                    "Payout method is required");
        }

        if (payoutDetails == null
                || payoutDetails.isBlank()) {

            throw new InvalidWithdrawalRequestException(
                    "Payout details are required");
        }

        String methodCode = payoutMethodCode
                .trim()
                .toUpperCase();

        String details = payoutDetails.trim();

        switch (methodCode) {

            case UPI -> validateUpi(details);

            case AMAZON_GIFT_CARD ->
                validateGiftCardEmail(
                        details,
                        "Amazon Gift Card");

            case GOOGLE_PLAY_GIFT_CARD ->
                validateGiftCardEmail(
                        details,
                        "Google Play Gift Card");

            default -> throw new InvalidWithdrawalRequestException(
                    "Unsupported payout method: " + methodCode);
        }
    }

    private void validateUpi(
            String payoutDetails) {

        if (!payoutDetails.matches(UPI_PATTERN)) {

            throw new InvalidWithdrawalRequestException(
                    "Invalid UPI ID");
        }
    }

    private void validateGiftCardEmail(
            String payoutDetails,
            String payoutMethodName) {

        if (!payoutDetails.matches(EMAIL_PATTERN)) {

            throw new InvalidWithdrawalRequestException(
                    "Invalid email address for "
                            + payoutMethodName);
        }
    }
}
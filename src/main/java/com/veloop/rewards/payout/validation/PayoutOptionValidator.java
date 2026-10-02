package com.veloop.rewards.payout.validation;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PayoutOptionValidator {

    private static final String INR = "INR";

    public void validate(
            PayoutMethod payoutMethod,
            PayoutOption payoutOption) {

        if (payoutMethod == null) {
            throw new InvalidWithdrawalRequestException(
                    "Payout method is required");
        }

        if (!Boolean.TRUE.equals(payoutMethod.getActive())) {
            throw new InvalidWithdrawalRequestException(
                    "Selected payout method is inactive");
        }

        if (payoutOption == null) {
            throw new InvalidWithdrawalRequestException(
                    "Payout option is required");
        }

        if (!Boolean.TRUE.equals(payoutOption.getActive())) {
            throw new InvalidWithdrawalRequestException(
                    "Selected payout option is inactive");
        }

        if (payoutOption.getMethod() == null
                || payoutOption.getMethod().getId() == null
                || payoutMethod.getId() == null
                || !payoutOption.getMethod()
                        .getId()
                        .equals(payoutMethod.getId())) {

            throw new InvalidWithdrawalRequestException(
                    "Payout option does not belong to selected payout method");
        }

        if (payoutOption.getPayoutAmount() == null
                || payoutOption.getPayoutAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidWithdrawalRequestException(
                    "Payout amount must be greater than zero");
        }

        if (payoutOption.getCurrency() == null
                || payoutOption.getCurrency().isBlank()) {

            throw new InvalidWithdrawalRequestException(
                    "Payout currency is required");
        }

        String currency = payoutOption.getCurrency().trim();

        if (!INR.equalsIgnoreCase(currency)) {
            throw new InvalidWithdrawalRequestException(
                    "Unsupported payout currency: " + currency);
        }

        if (payoutOption.getCurrencyAmount() == null
                || payoutOption.getCurrencyAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidWithdrawalRequestException(
                    "Required currency amount must be greater than zero");
        }
    }
}
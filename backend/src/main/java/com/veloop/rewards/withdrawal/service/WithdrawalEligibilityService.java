package com.veloop.rewards.withdrawal.service;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class WithdrawalEligibilityService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    public void validate(User user) {

        if (user == null) {
            throw new InvalidWithdrawalRequestException(
                    "User account could not be verified");
        }

        if (!ACTIVE_STATUS.equalsIgnoreCase(user.getAccountStatus())) {
            throw new InvalidWithdrawalRequestException(
                    "User account is not eligible for withdrawal");
        }

        if (!Boolean.TRUE.equals(user.getVerified())) {
            throw new InvalidWithdrawalRequestException(
                    "User account must be verified before withdrawal");
        }
    }
}
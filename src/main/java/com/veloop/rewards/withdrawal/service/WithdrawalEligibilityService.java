package com.veloop.rewards.withdrawal.service;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class WithdrawalEligibilityService {

    public void validate(User user) {

        if (user == null) {
            throw new InvalidWithdrawalRequestException(
                    "User account could not be verified");
        }

    }
}
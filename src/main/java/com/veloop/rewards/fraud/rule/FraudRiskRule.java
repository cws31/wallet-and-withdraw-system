package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;

import java.util.Optional;

public interface FraudRiskRule {

    String getRuleCode();

    int getRiskScore();

    Optional<String> evaluate(
            User user,
            Withdrawal withdrawal);
}
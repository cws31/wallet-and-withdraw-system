package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;

public interface FraudRiskRule {

    String getRuleCode();

    RiskRuleResult evaluate(
            User user,
            Withdrawal withdrawal);
}
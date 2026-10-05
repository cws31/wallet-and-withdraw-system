package com.veloop.rewards.fraud.service;

import java.time.LocalDateTime;

public interface FraudRiskRule {

    RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime);

    default RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime,
            Long payoutOptionId) {

        return evaluate(
                userId,
                evaluationTime);
    }
}
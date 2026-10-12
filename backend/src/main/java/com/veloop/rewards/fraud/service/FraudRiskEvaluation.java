package com.veloop.rewards.fraud.service;

import com.veloop.rewards.fraud.enums.FraudRiskDecision;

import java.util.List;

public record FraudRiskEvaluation(
        int totalRiskScore,
        FraudRiskDecision decision,
        List<RiskRuleResult> ruleResults) {

    public List<RiskRuleResult> triggeredRules() {
        return ruleResults.stream()
                .filter(RiskRuleResult::triggered)
                .toList();
    }
}

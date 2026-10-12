package com.veloop.rewards.fraud.service;

public record RiskRuleResult(
        boolean triggered,
        String ruleCode,
        int riskScore,
        String explanation) {

    public static RiskRuleResult notTriggered(String ruleCode, String explanation) {
        return new RiskRuleResult(false, ruleCode, 0, explanation);
    }
}

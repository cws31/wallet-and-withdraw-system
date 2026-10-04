package com.veloop.rewards.fraud.rule;

public class RiskRuleResult {

    private final boolean triggered;
    private final String ruleCode;
    private final int riskScore;
    private final String explanation;

    private RiskRuleResult(
            boolean triggered,
            String ruleCode,
            int riskScore,
            String explanation) {

        this.triggered = triggered;
        this.ruleCode = ruleCode;
        this.riskScore = riskScore;
        this.explanation = explanation;
    }

    public static RiskRuleResult notTriggered(
            String ruleCode) {

        return new RiskRuleResult(
                false,
                ruleCode,
                0,
                null);
    }

    public static RiskRuleResult triggered(
            String ruleCode,
            int riskScore,
            String explanation) {

        if (riskScore < 0) {
            throw new IllegalArgumentException(
                    "Risk score cannot be negative");
        }

        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException(
                    "Risk explanation is required");
        }

        return new RiskRuleResult(
                true,
                ruleCode,
                riskScore,
                explanation);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getExplanation() {
        return explanation;
    }
}
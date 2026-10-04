package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.entity.FraudRiskEvent;
import com.veloop.rewards.fraud.repository.FraudRiskEventRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RapidWithdrawalRule implements FraudRiskRule {

    private static final String RULE_CODE = "RAPID_WITHDRAWAL";

    private static final int WINDOW_MINUTES = 5;

    private final FraudRiskEventRepository fraudRiskEventRepository;

    public RapidWithdrawalRule(
            FraudRiskEventRepository fraudRiskEventRepository) {

        this.fraudRiskEventRepository = fraudRiskEventRepository;
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public RiskRuleResult evaluate(
            User user,
            Withdrawal withdrawal) {

        LocalDateTime windowStart = LocalDateTime.now()
                .minusMinutes(WINDOW_MINUTES);

        long recentRiskEvents = fraudRiskEventRepository
                .countByUserAndCreatedAtAfter(
                        user,
                        windowStart);

        long withdrawalCount = recentRiskEvents + 1;

        if (withdrawalCount < 2) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        int riskScore = calculateRiskScore(withdrawalCount);

        String explanation = "Detected "
                + withdrawalCount
                + " withdrawal requests within "
                + WINDOW_MINUTES
                + " minutes";

        return RiskRuleResult.triggered(
                RULE_CODE,
                riskScore,
                explanation);
    }

    private int calculateRiskScore(
            long withdrawalCount) {

        if (withdrawalCount == 2) {
            return 10;
        }

        if (withdrawalCount == 3) {
            return 25;
        }

        if (withdrawalCount == 4) {
            return 30;
        }

        return 35;
    }
}
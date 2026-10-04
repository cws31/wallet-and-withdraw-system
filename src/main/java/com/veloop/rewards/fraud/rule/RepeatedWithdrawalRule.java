package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.repository.FraudRiskEventRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RepeatedWithdrawalRule implements FraudRiskRule {

    private static final String RULE_CODE = "REPEATED_WITHDRAWAL";

    private static final int WINDOW_HOURS = 24;

    private final FraudRiskEventRepository fraudRiskEventRepository;

    public RepeatedWithdrawalRule(
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
                .minusHours(WINDOW_HOURS);

        long recentRiskEvents = fraudRiskEventRepository
                .countByUserAndCreatedAtAfter(
                        user,
                        windowStart);
        long withdrawalCount = recentRiskEvents + 1;

        if (withdrawalCount < 3) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        int riskScore = calculateRiskScore(withdrawalCount);

        String explanation = "Detected "
                + withdrawalCount
                + " withdrawal requests within "
                + WINDOW_HOURS
                + " hours";

        return RiskRuleResult.triggered(
                RULE_CODE,
                riskScore,
                explanation);
    }

    private int calculateRiskScore(
            long withdrawalCount) {

        if (withdrawalCount == 3) {
            return 10;
        }

        if (withdrawalCount == 4) {
            return 15;
        }

        if (withdrawalCount == 5) {
            return 20;
        }

        if (withdrawalCount <= 7) {
            return 25;
        }

        return 30;
    }
}
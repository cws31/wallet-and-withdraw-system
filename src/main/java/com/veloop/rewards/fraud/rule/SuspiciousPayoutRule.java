package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class SuspiciousPayoutRule implements FraudRiskRule {

    private static final String RULE_CODE = "SUSPICIOUS_PAYOUT_REQUESTS";

    private static final int WINDOW_MINUTES = 30;

    private final WithdrawalRepository withdrawalRepository;

    public SuspiciousPayoutRule(
            WithdrawalRepository withdrawalRepository) {

        this.withdrawalRepository = withdrawalRepository;
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public RiskRuleResult evaluate(
            User user,
            Withdrawal withdrawal) {

        if (withdrawal.getPayoutOption() == null) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        if (withdrawal.getCurrencyAmount() == null) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        Long payoutOptionId = withdrawal.getPayoutOption().getId();

        BigDecimal currencyAmount = withdrawal.getCurrencyAmount();

        if (payoutOptionId == null) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        LocalDateTime windowStart = LocalDateTime.now()
                .minusMinutes(WINDOW_MINUTES);

        long similarRequests = withdrawalRepository
                .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                        user.getId(),
                        payoutOptionId,
                        currencyAmount,
                        windowStart);

        long requestCount = similarRequests + 1;

        if (requestCount < 2) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        int riskScore = calculateRiskScore(requestCount);

        String explanation = "Detected "
                + requestCount
                + " similar payout requests within "
                + WINDOW_MINUTES
                + " minutes";

        return RiskRuleResult.triggered(
                RULE_CODE,
                riskScore,
                explanation);
    }

    private int calculateRiskScore(
            long requestCount) {

        if (requestCount == 2) {
            return 10;
        }

        if (requestCount == 3) {
            return 20;
        }

        if (requestCount == 4) {
            return 30;
        }

        if (requestCount <= 6) {
            return 35;
        }

        return 40;
    }
}
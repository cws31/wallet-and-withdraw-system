package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.repository.FraudRiskEventRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class RepeatedWithdrawalRule implements FraudRiskRule {

    private static final String RULE_CODE = "REPEATED_WITHDRAWAL";

    private static final int RISK_SCORE = 20;

    private static final int WINDOW_HOURS = 24;

    private static final int WITHDRAWAL_THRESHOLD = 5;

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
    public int getRiskScore() {
        return RISK_SCORE;
    }

    @Override
    public Optional<String> evaluate(
            User user,
            Withdrawal withdrawal) {

        LocalDateTime windowStart = LocalDateTime.now()
                .minusHours(WINDOW_HOURS);

        long recentRiskEvents = fraudRiskEventRepository
                .countByUserAndCreatedAtAfter(
                        user,
                        windowStart);

        if (recentRiskEvents >= WITHDRAWAL_THRESHOLD) {

            return Optional.of(
                    "User has repeated withdrawal activity "
                            + "within the last 24 hours");
        }

        return Optional.empty();
    }
}
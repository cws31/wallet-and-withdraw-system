package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.repository.FraudRiskEventRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class RapidWithdrawalRule implements FraudRiskRule {

    private static final String RULE_CODE = "RAPID_WITHDRAWAL";

    private static final int RISK_SCORE = 25;

    private static final int WINDOW_MINUTES = 5;

    private static final int WITHDRAWAL_THRESHOLD = 3;

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
    public int getRiskScore() {
        return RISK_SCORE;
    }

    @Override
    public Optional<String> evaluate(
            User user,
            Withdrawal withdrawal) {

        LocalDateTime windowStart = LocalDateTime.now()
                .minusMinutes(WINDOW_MINUTES);

        long recentRiskEvents = fraudRiskEventRepository
                .countByUserAndCreatedAtAfter(
                        user,
                        windowStart);

        if (recentRiskEvents >= WITHDRAWAL_THRESHOLD) {

            return Optional.of(
                    "User has multiple withdrawal risk events "
                            + "within a short time window");
        }

        return Optional.empty();
    }
}
package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class SuspiciousPayoutRule implements FraudRiskRule {

    private static final String RULE_CODE = "SUSPICIOUS_PAYOUT_REQUESTS";

    private static final int RISK_SCORE = 30;

    private static final int WINDOW_MINUTES = 30;

    private static final int REQUEST_THRESHOLD = 4;

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
    public int getRiskScore() {
        return RISK_SCORE;
    }

    @Override
    public Optional<String> evaluate(
            User user,
            Withdrawal withdrawal) {

        if (withdrawal.getPayoutOption() == null
                || withdrawal.getPayoutOption().getId() == null
                || withdrawal.getCurrencyAmount() == null) {

            return Optional.empty();
        }

        LocalDateTime windowStart = LocalDateTime.now()
                .minusMinutes(WINDOW_MINUTES);

        long similarRequests = withdrawalRepository
                .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                        user.getId(),
                        withdrawal.getPayoutOption().getId(),
                        withdrawal.getCurrencyAmount(),
                        windowStart);

        if (similarRequests >= REQUEST_THRESHOLD) {

            return Optional.of(
                    "User has repeatedly requested the same "
                            + "payout option and amount within "
                            + "a short time window");
        }

        return Optional.empty();
    }
}
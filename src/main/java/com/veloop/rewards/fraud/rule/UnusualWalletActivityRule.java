package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class UnusualWalletActivityRule implements FraudRiskRule {

    private static final String RULE_CODE = "UNUSUAL_WALLET_ACTIVITY";

    private static final int RISK_SCORE = 20;

    private static final int WINDOW_MINUTES = 15;

    private static final int TRANSACTION_THRESHOLD = 10;

    private final WalletTransactionRepository walletTransactionRepository;

    public UnusualWalletActivityRule(
            WalletTransactionRepository walletTransactionRepository) {

        this.walletTransactionRepository = walletTransactionRepository;
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

        long recentTransactions = walletTransactionRepository
                .countByUserIdAndCreatedAtAfter(
                        user.getId(),
                        windowStart);

        if (recentTransactions >= TRANSACTION_THRESHOLD) {

            return Optional.of(
                    "User has unusually high wallet "
                            + "transaction activity within "
                            + "a short time window");
        }

        return Optional.empty();
    }
}
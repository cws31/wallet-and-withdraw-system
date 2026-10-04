package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UnusualWalletActivityRule implements FraudRiskRule {

    private static final String RULE_CODE = "UNUSUAL_WALLET_ACTIVITY";

    private static final int WINDOW_MINUTES = 15;

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
    public RiskRuleResult evaluate(
            User user,
            Withdrawal withdrawal) {

        LocalDateTime windowStart = LocalDateTime.now()
                .minusMinutes(WINDOW_MINUTES);

        long transactionCount = walletTransactionRepository
                .countByUserIdAndCreatedAtAfter(
                        user.getId(),
                        windowStart);

        if (transactionCount < 5) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE);
        }

        int riskScore = calculateRiskScore(transactionCount);

        String explanation = "Detected "
                + transactionCount
                + " wallet transactions within "
                + WINDOW_MINUTES
                + " minutes";

        return RiskRuleResult.triggered(
                RULE_CODE,
                riskScore,
                explanation);
    }

    private int calculateRiskScore(
            long transactionCount) {

        if (transactionCount <= 6) {
            return 10;
        }

        if (transactionCount <= 8) {
            return 15;
        }

        if (transactionCount <= 10) {
            return 20;
        }

        if (transactionCount <= 15) {
            return 25;
        }

        return 30;
    }
}
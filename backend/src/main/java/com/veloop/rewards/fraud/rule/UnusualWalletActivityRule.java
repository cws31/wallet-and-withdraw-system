package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.FraudRiskRule;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UnusualWalletActivityRule
        implements FraudRiskRule {

    public static final String RULE_CODE = "UNUSUAL_WALLET_ACTIVITY";

    private final WalletTransactionRepository walletTransactionRepository;

    private final FraudRuleProperties properties;

    public UnusualWalletActivityRule(
            WalletTransactionRepository walletTransactionRepository,
            FraudRuleProperties properties) {

        this.walletTransactionRepository = walletTransactionRepository;

        this.properties = properties;
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime) {

        FraudRuleProperties.UnusualWalletActivity configuration = properties.getUnusualWalletActivity();

        validateConfiguration(configuration);

        LocalDateTime windowStart = evaluationTime.minusMinutes(
                configuration.getWindowMinutes());

        long completedTransactions = walletTransactionRepository
                .countByUserIdAndStatusAndCreatedAtAfter(
                        userId,
                        TransactionStatus.COMPLETED,
                        windowStart);

        /*
         * The current withdrawal request is deliberately
         * not added here.
         *
         * Fraud evaluation occurs before the new wallet
         * transaction is completed, so only authoritative
         * historical wallet activity is measured.
         */
        long observedTransactions = completedTransactions;

        double severity = calculateSeverity(
                observedTransactions,
                configuration.getBaseline(),
                configuration.getMaxExpected());

        int riskScore = calculateRiskScore(
                severity,
                configuration.getMaxScore());

        if (riskScore <= 0) {

            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "Detected "
                            + observedTransactions
                            + " completed wallet transaction(s) within "
                            + configuration.getWindowMinutes()
                            + " minutes. Configured baseline is "
                            + configuration.getBaseline()
                            + ". Calculated severity is "
                            + format(severity)
                            + ". Dynamic risk contribution is 0.");
        }

        return new RiskRuleResult(
                true,
                RULE_CODE,
                riskScore,
                "Detected "
                        + observedTransactions
                        + " completed wallet transaction(s) within "
                        + configuration.getWindowMinutes()
                        + " minutes. Configured baseline is "
                        + configuration.getBaseline()
                        + ", maximum expected activity is "
                        + configuration.getMaxExpected()
                        + ", calculated severity is "
                        + format(severity)
                        + ", and dynamic risk contribution is "
                        + riskScore
                        + ".");
    }

    static double calculateSeverity(
            long observed,
            int baseline,
            int maximumExpected) {

        if (observed <= baseline) {
            return 0.0d;
        }

        double severity = (double) (observed - baseline)
                / (maximumExpected - baseline);

        return Math.min(
                1.0d,
                Math.max(0.0d, severity));
    }

    static int calculateRiskScore(
            double severity,
            int maximumRuleScore) {

        return (int) Math.round(
                maximumRuleScore
                        * severity
                        * severity);
    }

    private void validateConfiguration(
            FraudRuleProperties.UnusualWalletActivity configuration) {

        if (configuration.getWindowMinutes() <= 0
                || configuration.getBaseline() < 0
                || configuration.getMaxExpected() <= configuration.getBaseline()
                || configuration.getMaxScore() < 0) {

            throw new IllegalStateException(
                    "Invalid unusual wallet activity "
                            + "fraud rule configuration");
        }
    }

    private String format(double value) {

        return String.format(
                java.util.Locale.ROOT,
                "%.4f",
                value);
    }
}
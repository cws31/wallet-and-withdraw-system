package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.FraudRiskRule;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MultipleSuspiciousPayoutPatternRule
        implements FraudRiskRule {

    public static final String RULE_CODE = "MULTIPLE_SUSPICIOUS_PAYOUT_PATTERN";

    private final WithdrawalRepository withdrawalRepository;
    private final FraudRuleProperties properties;

    public MultipleSuspiciousPayoutPatternRule(
            WithdrawalRepository withdrawalRepository,
            FraudRuleProperties properties) {

        this.withdrawalRepository = withdrawalRepository;
        this.properties = properties;
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime) {

        return RiskRuleResult.notTriggered(
                RULE_CODE,
                "Current payout option is not available "
                        + "for multiple suspicious payout pattern evaluation. "
                        + "Dynamic risk contribution is 0.");
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime,
            Long payoutOptionId) {

        FraudRuleProperties.MultipleSuspiciousPayoutPattern configuration = properties
                .getMultipleSuspiciousPayoutPattern();

        validateConfiguration(configuration);

        if (payoutOptionId == null) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "Current payout option is not available "
                            + "for multiple suspicious payout pattern evaluation. "
                            + "Dynamic risk contribution is 0.");
        }

        LocalDateTime windowStart = evaluationTime.minusMinutes(
                configuration.getWindowMinutes());

        long previousDistinctOptions = withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        userId,
                        windowStart);

        boolean currentOptionAlreadyUsed = withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        userId,
                        payoutOptionId,
                        windowStart);

        long observedDistinctOptions = currentOptionAlreadyUsed
                ? previousDistinctOptions
                : previousDistinctOptions + 1;

        double severity = calculateSeverity(
                observedDistinctOptions,
                configuration.getBaseline(),
                configuration.getMaxExpected());

        int riskScore = calculateRiskScore(
                severity,
                configuration.getMaxScore());

        if (riskScore <= 0) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "Detected "
                            + observedDistinctOptions
                            + " distinct payout option(s) within "
                            + configuration.getWindowMinutes()
                            + " minute(s). Configured baseline is "
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
                        + observedDistinctOptions
                        + " distinct payout option(s) within "
                        + configuration.getWindowMinutes()
                        + " minute(s). Configured baseline is "
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

        if (maximumExpected <= baseline) {
            return 1.0d;
        }

        double severity = (double) (observed - baseline)
                / (double) (maximumExpected - baseline);

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
            FraudRuleProperties.MultipleSuspiciousPayoutPattern configuration) {

        if (configuration.getWindowMinutes() <= 0
                || configuration.getBaseline() < 0
                || configuration.getMaxExpected() <= configuration.getBaseline()
                || configuration.getMaxScore() < 0) {

            throw new IllegalStateException(
                    "Invalid multiple suspicious payout pattern "
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
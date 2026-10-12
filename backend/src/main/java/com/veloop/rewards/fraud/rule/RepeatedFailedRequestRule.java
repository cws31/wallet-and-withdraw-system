package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.FraudRiskRule;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RepeatedFailedRequestRule
        implements FraudRiskRule {

    public static final String RULE_CODE = "REPEATED_FAILED_REQUEST";

    private final WithdrawalRepository withdrawalRepository;
    private final FraudRuleProperties properties;

    public RepeatedFailedRequestRule(
            WithdrawalRepository withdrawalRepository,
            FraudRuleProperties properties) {

        this.withdrawalRepository = withdrawalRepository;

        this.properties = properties;
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime) {

        FraudRuleProperties.FailedRequests configuration = properties.getFailedRequests();

        validateConfiguration(configuration);

        LocalDateTime windowStart = evaluationTime.minusMinutes(
                configuration.getWindowMinutes());

        long previousFailedRequests = withdrawalRepository
                .countByUserIdAndStatusAndCreatedAtAfter(
                        userId,
                        WithdrawalStatus.REJECTED,
                        windowStart);

        long observedFailedRequests = previousFailedRequests;

        double severity = calculateSeverity(
                observedFailedRequests,
                configuration.getBaseline(),
                configuration.getMaxExpected());

        int riskScore = calculateRiskScore(
                severity,
                configuration.getMaxScore());

        if (riskScore <= 0) {

            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "Detected "
                            + observedFailedRequests
                            + " rejected withdrawal request(s) within "
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
                        + observedFailedRequests
                        + " rejected withdrawal request(s) within "
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
            FraudRuleProperties.FailedRequests configuration) {

        if (configuration.getWindowMinutes() <= 0
                || configuration.getBaseline() < 0
                || configuration.getMaxExpected() <= configuration.getBaseline()
                || configuration.getMaxScore() < 0) {

            throw new IllegalStateException(
                    "Invalid repeated failed request "
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
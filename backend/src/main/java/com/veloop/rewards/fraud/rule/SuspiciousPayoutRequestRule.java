package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.FraudRiskRule;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SuspiciousPayoutRequestRule
        implements FraudRiskRule {

    public static final String RULE_CODE = "SUSPICIOUS_PAYOUT_REQUEST";

    private final WithdrawalRepository withdrawalRepository;
    private final FraudRuleProperties properties;

    public SuspiciousPayoutRequestRule(
            WithdrawalRepository withdrawalRepository,
            FraudRuleProperties properties) {

        this.withdrawalRepository = withdrawalRepository;

        this.properties = properties;
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime) {

        /*
         * This rule requires the current payout option.
         *
         * If the rule is evaluated through the legacy
         * interface, there is no payout option context,
         * so the rule safely remains inactive.
         */
        return RiskRuleResult.notTriggered(
                RULE_CODE,
                "Current payout option is not available "
                        + "for suspicious payout request evaluation. "
                        + "Dynamic risk contribution is 0.");
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime,
            Long payoutOptionId) {

        FraudRuleProperties.SuspiciousPayout configuration = properties.getSuspiciousPayout();

        validateConfiguration(configuration);

        if (payoutOptionId == null) {

            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "Current payout option is not available "
                            + "for suspicious payout request evaluation. "
                            + "Dynamic risk contribution is 0.");
        }

        LocalDateTime windowStart = evaluationTime.minusMinutes(
                configuration.getWindowMinutes());

        long previousMatchingRequests = withdrawalRepository
                .countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        userId,
                        payoutOptionId,
                        windowStart);

        /*
         * The current request already contains a selected
         * payout option and is being evaluated before the
         * withdrawal is persisted.
         *
         * Therefore it is included as the current observed
         * request.
         */
        long observedRequests = previousMatchingRequests + 1;

        double severity = calculateSeverity(
                observedRequests,
                configuration.getBaseline(),
                configuration.getMaxExpected());

        int riskScore = calculateRiskScore(
                severity,
                configuration.getMaxScore());

        if (riskScore <= 0) {

            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "Detected "
                            + observedRequests
                            + " request(s) for payout option "
                            + payoutOptionId
                            + " within "
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
                        + observedRequests
                        + " request(s) for payout option "
                        + payoutOptionId
                        + " within "
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
            FraudRuleProperties.SuspiciousPayout configuration) {

        if (configuration.getWindowMinutes() <= 0
                || configuration.getBaseline() < 0
                || configuration.getMaxExpected() <= configuration.getBaseline()
                || configuration.getMaxScore() < 0) {

            throw new IllegalStateException(
                    "Invalid suspicious payout request "
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
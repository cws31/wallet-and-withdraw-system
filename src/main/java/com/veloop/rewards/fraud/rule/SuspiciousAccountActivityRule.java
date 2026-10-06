package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.auth.repository.AuthenticationAttemptRepository;
import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.FraudRiskRule;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SuspiciousAccountActivityRule implements FraudRiskRule {

    private static final String RULE_CODE = "SUSPICIOUS_ACCOUNT_ACTIVITY";

    private final AuthenticationAttemptRepository authenticationAttemptRepository;

    private final FraudRuleProperties properties;

    public SuspiciousAccountActivityRule(
            AuthenticationAttemptRepository authenticationAttemptRepository,
            FraudRuleProperties properties) {

        this.authenticationAttemptRepository = authenticationAttemptRepository;

        this.properties = properties;
    }

    @Override
    public RiskRuleResult evaluate(
            Long userId,
            LocalDateTime evaluationTime) {

        FraudRuleProperties.SuspiciousAccountActivity config = properties.getSuspiciousAccountActivity();

        LocalDateTime windowStart = evaluationTime.minusMinutes(
                config.getWindowMinutes());

        long failedAttempts = authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        userId,
                        windowStart);

        int riskScore = calculateRiskScore(
                failedAttempts,
                config);

        if (riskScore == 0) {
            return RiskRuleResult.notTriggered(
                    RULE_CODE,
                    "No suspicious authentication activity detected");
        }

        return new RiskRuleResult(
                true,
                RULE_CODE,
                riskScore,
                "Detected "
                        + failedAttempts
                        + " failed authentication attempt(s) within "
                        + config.getWindowMinutes()
                        + " minute(s); calculated risk score="
                        + riskScore);
    }

    private int calculateRiskScore(
            long observed,
            FraudRuleProperties.SuspiciousAccountActivity config) {

        int baseline = config.getBaseline();
        int maxExpected = config.getMaxExpected();
        int maxScore = config.getMaxScore();

        if (observed <= baseline) {
            return 0;
        }

        if (maxExpected <= baseline) {
            return maxScore;
        }

        double severity = (double) (observed - baseline)
                / (double) (maxExpected - baseline);

        severity = Math.max(
                0.0,
                Math.min(1.0, severity));

        double risk = maxScore * severity * severity;

        return (int) Math.round(risk);
    }
}
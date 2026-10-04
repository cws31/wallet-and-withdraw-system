package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.audit.repository.AuditLogRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class RepeatedFailedRequestRule implements FraudRiskRule {

    private static final String RULE_CODE = "REPEATED_FAILED_REQUESTS";

    private static final int RISK_SCORE = 15;

    private static final int WINDOW_MINUTES = 10;

    private static final int FAILURE_THRESHOLD = 5;

    private static final String FAILED_WITHDRAWAL_ACTION = "WITHDRAWAL_REQUEST_FAILED";

    private final AuditLogRepository auditLogRepository;

    public RepeatedFailedRequestRule(
            AuditLogRepository auditLogRepository) {

        this.auditLogRepository = auditLogRepository;
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

        long failedRequests = auditLogRepository
                .countByTargetUserIdAndActionAndCreatedAtAfter(
                        user.getId(),
                        FAILED_WITHDRAWAL_ACTION,
                        windowStart);

        if (failedRequests >= FAILURE_THRESHOLD) {

            return Optional.of(
                    "User has repeated failed withdrawal "
                            + "requests within a short time window");
        }

        return Optional.empty();
    }
}
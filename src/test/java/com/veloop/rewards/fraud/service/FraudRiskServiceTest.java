package com.veloop.rewards.fraud.service;

import com.veloop.rewards.fraud.config.FraudRiskProperties;
import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FraudRiskServiceTest {

    @Test
    void shouldPassCurrentPayoutOptionToFraudRules() {

        CapturingRule rule = new CapturingRule();

        FraudRiskProperties properties = new FraudRiskProperties();

        FraudRiskService service = new FraudRiskService(
                List.of(rule),
                properties,
                null,
                null);

        Long userId = 100L;
        Long payoutOptionId = 55L;

        LocalDateTime evaluationTime = LocalDateTime.of(
                2026,
                10,
                6,
                22,
                0);

        FraudRiskEvaluation evaluation = service.evaluate(
                userId,
                evaluationTime,
                payoutOptionId);

        assertNotNull(evaluation);

        assertEquals(
                userId,
                rule.receivedUserId);

        assertEquals(
                evaluationTime,
                rule.receivedEvaluationTime);

        assertEquals(
                payoutOptionId,
                rule.receivedPayoutOptionId);

        assertEquals(
                10,
                evaluation.totalRiskScore());

        assertEquals(
                FraudRiskDecision.ALLOW,
                evaluation.decision());
    }

    private static class CapturingRule
            implements FraudRiskRule {

        private Long receivedUserId;

        private LocalDateTime receivedEvaluationTime;

        private Long receivedPayoutOptionId;

        @Override
        public RiskRuleResult evaluate(
                Long userId,
                LocalDateTime evaluationTime) {

            return RiskRuleResult.notTriggered(
                    "TEST_RULE",
                    "Test evaluation");
        }

        @Override
        public RiskRuleResult evaluate(
                Long userId,
                LocalDateTime evaluationTime,
                Long payoutOptionId) {

            this.receivedUserId = userId;

            this.receivedEvaluationTime = evaluationTime;

            this.receivedPayoutOptionId = payoutOptionId;

            return new RiskRuleResult(
                    true,
                    "TEST_RULE",
                    10,
                    "Test rule contributed 10");
        }
    }
}
package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RapidWithdrawalRuleTest {

        @Mock
        private WithdrawalRepository withdrawalRepository;

        private FraudRuleProperties properties;
        private RapidWithdrawalRule rule;
        private final LocalDateTime evaluationTime = LocalDateTime.of(2026, 10, 5, 12, 0);

        @BeforeEach
        void setUp() {
                properties = new FraudRuleProperties();
                rule = new RapidWithdrawalRule(
                                withdrawalRepository,
                                properties);
        }

        @Test
        void shouldAllowNormalActivity() {
                when(withdrawalRepository.countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusMinutes(5)))
                                .thenReturn(0L);

                RiskRuleResult result = rule.evaluate(1L, evaluationTime);

                assertFalse(result.triggered());
                assertEquals(0, result.riskScore());
        }

        @Test
        void shouldCalculateProgressivelyForElevatedActivity() {
                when(withdrawalRepository.countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusMinutes(5)))
                                .thenReturn(1L, 2L, 3L);

                assertEquals(
                                3,
                                rule.evaluate(1L, evaluationTime).riskScore());

                assertEquals(
                                10,
                                rule.evaluate(1L, evaluationTime).riskScore());

                assertEquals(
                                23,
                                rule.evaluate(1L, evaluationTime).riskScore());
        }

        @Test
        void shouldReachConfiguredMaximumAtOrAboveMaximumExpectedActivity() {
                when(withdrawalRepository.countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusMinutes(5)))
                                .thenReturn(4L, 9L);

                assertEquals(
                                40,
                                rule.evaluate(1L, evaluationTime).riskScore());

                assertEquals(
                                40,
                                rule.evaluate(1L, evaluationTime).riskScore());
        }

        @Test
        void shouldRespectConfigurationChanges() {
                when(withdrawalRepository.countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusMinutes(5)))
                                .thenReturn(3L, 3L);

                properties.getRapidWithdrawal().setMaxScore(40);

                int scoreWithFortyMax = rule.evaluate(1L, evaluationTime).riskScore();

                properties.getRapidWithdrawal().setMaxScore(60);

                int scoreWithSixtyMax = rule.evaluate(1L, evaluationTime).riskScore();

                assertEquals(23, scoreWithFortyMax);
                assertEquals(34, scoreWithSixtyMax);
                assertNotEquals(
                                scoreWithFortyMax,
                                scoreWithSixtyMax);
        }

        @Test
        void shouldRespectConfiguredWindow() {
                when(withdrawalRepository.countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusMinutes(10)))
                                .thenReturn(2L);

                properties.getRapidWithdrawal().setWindowMinutes(10);

                RiskRuleResult result = rule.evaluate(1L, evaluationTime);

                assertEquals(10, result.riskScore());
        }

        @Test
        void shouldClampSeverityAtZeroAndOne() {
                assertEquals(
                                0.0,
                                RapidWithdrawalRule.calculateSeverity(
                                                1,
                                                1,
                                                5));

                assertEquals(
                                0.0,
                                RapidWithdrawalRule.calculateSeverity(
                                                0,
                                                1,
                                                5));

                assertEquals(
                                1.0,
                                RapidWithdrawalRule.calculateSeverity(
                                                5,
                                                1,
                                                5));

                assertEquals(
                                1.0,
                                RapidWithdrawalRule.calculateSeverity(
                                                50,
                                                1,
                                                5));
        }
}
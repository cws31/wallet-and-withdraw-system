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
class RepeatedWithdrawalRuleTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    private FraudRuleProperties properties;
    private RepeatedWithdrawalRule rule;

    private final LocalDateTime evaluationTime = LocalDateTime.of(2026, 10, 5, 12, 0);

    @BeforeEach
    void setUp() {
        properties = new FraudRuleProperties();

        rule = new RepeatedWithdrawalRule(
                withdrawalRepository,
                properties);
    }

    @Test
    void shouldAllowNormalActivity() {

        when(
                withdrawalRepository
                        .countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusHours(24)))
                .thenReturn(1L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertFalse(result.triggered());
        assertEquals(0, result.riskScore());
    }

    @Test
    void shouldCalculateProgressivelyForRepeatedActivity() {

        when(
                withdrawalRepository
                        .countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusHours(24)))
                .thenReturn(
                        2L,
                        3L,
                        4L);

        /*
         * Configuration:
         * baseline = 2
         * maxExpected = 8
         * maxScore = 40
         *
         * Observed = previous withdrawals + current request.
         *
         * Observed = 3:
         * severity = (3 - 2) / (8 - 2)
         * = 1 / 6
         * risk = 40 * (1 / 6)^2
         * ≈ 1
         */
        assertEquals(
                1,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

        /*
         * Observed = 4:
         * severity = (4 - 2) / (8 - 2)
         * = 2 / 6
         * risk = 40 * (2 / 6)^2
         * ≈ 4
         */
        assertEquals(
                4,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

        /*
         * Observed = 5:
         * severity = (5 - 2) / (8 - 2)
         * = 3 / 6
         * risk = 40 * 0.5^2
         * = 10
         */
        assertEquals(
                10,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());
    }

    @Test
    void shouldReachConfiguredMaximumAtOrAboveMaximumExpectedActivity() {

        when(
                withdrawalRepository
                        .countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusHours(24)))
                .thenReturn(
                        7L,
                        20L);

        /*
         * Previous = 7
         * Current = 1
         * Observed = 8
         *
         * This is exactly maxExpected,
         * therefore severity = 1 and risk = maxScore.
         */
        assertEquals(
                40,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

        /*
         * Observed activity above maxExpected
         * must remain capped at maxScore.
         */
        assertEquals(
                40,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());
    }

    @Test
    void shouldRespectConfigurationChanges() {

        when(
                withdrawalRepository
                        .countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusHours(24)))
                .thenReturn(
                        4L,
                        4L);

        /*
         * Previous = 4
         * Current = 1
         * Observed = 5
         *
         * severity = (5 - 2) / (8 - 2)
         * = 3 / 6
         * = 0.5
         *
         * With maxScore = 40:
         * 40 * 0.5^2 = 10
         */
        properties
                .getRepeatedWithdrawal()
                .setMaxScore(40);

        int scoreWithFortyMax = rule.evaluate(
                1L,
                evaluationTime)
                .riskScore();

        /*
         * With maxScore = 60:
         * 60 * 0.5^2 = 15
         */
        properties
                .getRepeatedWithdrawal()
                .setMaxScore(60);

        int scoreWithSixtyMax = rule.evaluate(
                1L,
                evaluationTime)
                .riskScore();

        assertEquals(
                10,
                scoreWithFortyMax);

        assertEquals(
                15,
                scoreWithSixtyMax);

        assertNotEquals(
                scoreWithFortyMax,
                scoreWithSixtyMax);
    }

    @Test
    void shouldRespectConfiguredWindow() {

        when(
                withdrawalRepository
                        .countByUserIdAndCreatedAtAfter(
                                1L,
                                evaluationTime.minusHours(12)))
                .thenReturn(3L);

        properties
                .getRepeatedWithdrawal()
                .setWindowHours(12);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        /*
         * Previous = 3
         * Current = 1
         * Observed = 4
         *
         * severity = (4 - 2) / (8 - 2)
         * = 2 / 6
         *
         * risk = 40 * (2 / 6)^2
         * ≈ 4
         */
        assertEquals(
                4,
                result.riskScore());
    }

    @Test
    void shouldClampSeverityAtZeroAndOne() {

        assertEquals(
                0.0,
                RepeatedWithdrawalRule.calculateSeverity(
                        2,
                        2,
                        8));

        assertEquals(
                0.0,
                RepeatedWithdrawalRule.calculateSeverity(
                        0,
                        2,
                        8));

        assertEquals(
                1.0,
                RepeatedWithdrawalRule.calculateSeverity(
                        8,
                        2,
                        8));

        assertEquals(
                1.0,
                RepeatedWithdrawalRule.calculateSeverity(
                        50,
                        2,
                        8));
    }
}
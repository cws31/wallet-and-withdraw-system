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
class SuspiciousPayoutRequestRuleTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    private FraudRuleProperties properties;

    private SuspiciousPayoutRequestRule rule;

    private final LocalDateTime evaluationTime = LocalDateTime.of(
            2026,
            10,
            5,
            12,
            0);

    @BeforeEach
    void setUp() {

        properties = new FraudRuleProperties();

        rule = new SuspiciousPayoutRequestRule(
                withdrawalRepository,
                properties);
    }

    @Test
    void shouldAllowNormalPayoutOptionActivity() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                                1L,
                                10L,
                                evaluationTime.minusMinutes(30)))
                .thenReturn(0L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                10L);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskScore());
    }

    @Test
    void shouldCalculateProgressivelyForRepeatedPayoutOptionRequests() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                                1L,
                                10L,
                                evaluationTime.minusMinutes(30)))
                .thenReturn(
                        1L,
                        2L,
                        3L);

        /*
         * Current request is included.
         *
         * baseline = 1
         * maxExpected = 7
         * maxScore = 45
         *
         * previous = 1
         * observed = 2
         *
         * severity = 1 / 6
         * risk ≈ 1.25
         * rounded = 1
         */
        assertEquals(
                1,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L)
                        .riskScore());

        /*
         * previous = 2
         * observed = 3
         *
         * severity = 2 / 6
         * risk = 45 * (1/3)^2
         * = 5
         */
        assertEquals(
                5,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L)
                        .riskScore());

        /*
         * previous = 3
         * observed = 4
         *
         * severity = 3 / 6
         * risk = 45 * 0.5^2
         * = 11.25
         * = 11
         */
        assertEquals(
                11,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L)
                        .riskScore());
    }

    @Test
    void shouldReachConfiguredMaximumAtOrAboveMaximumExpectedActivity() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                                1L,
                                10L,
                                evaluationTime.minusMinutes(30)))
                .thenReturn(
                        6L,
                        20L);

        /*
         * previous = 6
         * current = 1
         * observed = 7
         */
        assertEquals(
                45,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L)
                        .riskScore());

        /*
         * observed = 21
         * Severity is clamped to 1.
         */
        assertEquals(
                45,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L)
                        .riskScore());
    }

    @Test
    void shouldRespectConfigurationChanges() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                                1L,
                                10L,
                                evaluationTime.minusMinutes(30)))
                .thenReturn(
                        3L,
                        3L);

        /*
         * previous = 3
         * current = 1
         * observed = 4
         *
         * baseline = 1
         * maxExpected = 7
         * severity = 3 / 6 = 0.5
         */
        properties
                .getSuspiciousPayout()
                .setMaxScore(45);

        int scoreWithFortyFiveMax = rule.evaluate(
                1L,
                evaluationTime,
                10L)
                .riskScore();

        properties
                .getSuspiciousPayout()
                .setMaxScore(90);

        int scoreWithNinetyMax = rule.evaluate(
                1L,
                evaluationTime,
                10L)
                .riskScore();

        assertEquals(
                11,
                scoreWithFortyFiveMax);

        assertEquals(
                23,
                scoreWithNinetyMax);

        assertNotEquals(
                scoreWithFortyFiveMax,
                scoreWithNinetyMax);
    }

    @Test
    void shouldRespectConfiguredWindow() {

        properties
                .getSuspiciousPayout()
                .setWindowMinutes(5);

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                                1L,
                                10L,
                                evaluationTime.minusMinutes(5)))
                .thenReturn(3L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                10L);

        /*
         * previous = 3
         * current = 1
         * observed = 4
         *
         * severity = 0.5
         * risk = 11.25 -> 11
         */
        assertEquals(
                11,
                result.riskScore());
    }

    @Test
    void shouldRemainInactiveWhenPayoutOptionIsUnavailable() {

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                null);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskScore());
    }

    @Test
    void shouldClampSeverityAtZeroAndOne() {

        assertEquals(
                0.0,
                SuspiciousPayoutRequestRule
                        .calculateSeverity(
                                1,
                                1,
                                7));

        assertEquals(
                0.0,
                SuspiciousPayoutRequestRule
                        .calculateSeverity(
                                0,
                                1,
                                7));

        assertEquals(
                1.0,
                SuspiciousPayoutRequestRule
                        .calculateSeverity(
                                7,
                                1,
                                7));

        assertEquals(
                1.0,
                SuspiciousPayoutRequestRule
                        .calculateSeverity(
                                50,
                                1,
                                7));
    }
}
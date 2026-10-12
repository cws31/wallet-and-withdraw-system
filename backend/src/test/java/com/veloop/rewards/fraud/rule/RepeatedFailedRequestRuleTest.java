package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
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
class RepeatedFailedRequestRuleTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    private FraudRuleProperties properties;
    private RepeatedFailedRequestRule rule;

    private final LocalDateTime evaluationTime = LocalDateTime.of(
            2026,
            10,
            5,
            12,
            0);

    @BeforeEach
    void setUp() {

        properties = new FraudRuleProperties();

        rule = new RepeatedFailedRequestRule(
                withdrawalRepository,
                properties);
    }

    @Test
    void shouldAllowNormalFailedRequestHistory() {

        when(
                withdrawalRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                WithdrawalStatus.REJECTED,
                                evaluationTime.minusMinutes(10)))
                .thenReturn(2L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertFalse(result.triggered());
        assertEquals(
                0,
                result.riskScore());
    }

    @Test
    void shouldCalculateProgressivelyForRepeatedFailures() {

        when(
                withdrawalRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                WithdrawalStatus.REJECTED,
                                evaluationTime.minusMinutes(10)))
                .thenReturn(
                        3L,
                        4L,
                        5L);

        assertEquals(
                1,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

        assertEquals(
                4,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

        assertEquals(
                9,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());
    }

    @Test
    void shouldReachConfiguredMaximumAtOrAboveMaximumExpectedFailures() {

        when(
                withdrawalRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                WithdrawalStatus.REJECTED,
                                evaluationTime.minusMinutes(10)))
                .thenReturn(
                        8L,
                        20L);

        assertEquals(
                35,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

        assertEquals(
                35,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());
    }

    @Test
    void shouldRespectConfigurationChanges() {

        when(
                withdrawalRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                WithdrawalStatus.REJECTED,
                                evaluationTime.minusMinutes(10)))
                .thenReturn(
                        5L,
                        5L);

        properties
                .getFailedRequests()
                .setMaxScore(35);

        int scoreWithThirtyFiveMax = rule.evaluate(
                1L,
                evaluationTime)
                .riskScore();

        properties
                .getFailedRequests()
                .setMaxScore(60);

        int scoreWithSixtyMax = rule.evaluate(
                1L,
                evaluationTime)
                .riskScore();

        assertEquals(
                9,
                scoreWithThirtyFiveMax);

        assertEquals(
                15,
                scoreWithSixtyMax);

        assertNotEquals(
                scoreWithThirtyFiveMax,
                scoreWithSixtyMax);
    }

    @Test
    void shouldRespectConfiguredWindow() {

        when(
                withdrawalRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                WithdrawalStatus.REJECTED,
                                evaluationTime.minusMinutes(5)))
                .thenReturn(4L);

        properties
                .getFailedRequests()
                .setWindowMinutes(5);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertEquals(
                4,
                result.riskScore());
    }

    @Test
    void shouldClampSeverityAtZeroAndOne() {

        assertEquals(
                0.0,
                RepeatedFailedRequestRule
                        .calculateSeverity(
                                2,
                                2,
                                8));

        assertEquals(
                0.0,
                RepeatedFailedRequestRule
                        .calculateSeverity(
                                0,
                                2,
                                8));

        assertEquals(
                1.0,
                RepeatedFailedRequestRule
                        .calculateSeverity(
                                8,
                                2,
                                8));

        assertEquals(
                1.0,
                RepeatedFailedRequestRule
                        .calculateSeverity(
                                50,
                                2,
                                8));
    }
}
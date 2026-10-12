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
class MultipleSuspiciousPayoutPatternRuleTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    private FraudRuleProperties properties;

    private MultipleSuspiciousPayoutPatternRule rule;

    private final LocalDateTime evaluationTime = LocalDateTime.of(2026, 10, 6, 12, 0);

    @BeforeEach
    void setUp() {

        properties = new FraudRuleProperties();

        rule = new MultipleSuspiciousPayoutPatternRule(
                withdrawalRepository,
                properties);
    }

    @Test
    void shouldAllowSinglePayoutOption() {

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(0L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        10L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(false);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                10L);

        assertFalse(result.triggered());
        assertEquals(0, result.riskScore());
    }

    @Test
    void shouldCalculateProgressiveRiskForMultipleDistinctOptions() {

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(1L, 2L, 3L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        10L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(false, false, false);

        assertEquals(
                4,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L).riskScore());

        assertEquals(
                18,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L).riskScore());

        assertEquals(
                40,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L).riskScore());
    }

    @Test
    void shouldReachMaximumAtOrAboveMaximumExpectedOptions() {

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(4L, 7L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        10L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(false, false);

        assertEquals(
                40,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L).riskScore());

        assertEquals(
                40,
                rule.evaluate(
                        1L,
                        evaluationTime,
                        10L).riskScore());
    }

    @Test
    void shouldNotIncreaseDistinctCountWhenCurrentOptionWasAlreadyUsed() {

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(3L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        10L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(true);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                10L);

        assertTrue(result.triggered());

        assertEquals(
                18,
                result.riskScore());
    }

    @Test
    void shouldIncludeCurrentNewOptionInEvaluation() {

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(3L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        40L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(false);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                40L);

        assertTrue(result.triggered());

        assertEquals(
                40,
                result.riskScore());
    }

    @Test
    void shouldRespectConfigurationChanges() {

        properties
                .getMultipleSuspiciousPayoutPattern()
                .setMaxScore(60);

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(2L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        10L,
                        evaluationTime.minusMinutes(30)))
                .thenReturn(false);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                10L);

        assertEquals(
                27,
                result.riskScore());
    }

    @Test
    void shouldRespectConfiguredWindow() {

        properties
                .getMultipleSuspiciousPayoutPattern()
                .setWindowMinutes(60);

        when(withdrawalRepository
                .countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        1L,
                        evaluationTime.minusMinutes(60)))
                .thenReturn(2L);

        when(withdrawalRepository
                .existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        1L,
                        10L,
                        evaluationTime.minusMinutes(60)))
                .thenReturn(false);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                10L);

        assertEquals(
                18,
                result.riskScore());
    }

    @Test
    void shouldReturnZeroWhenPayoutOptionIsMissing() {

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime,
                null);

        assertFalse(result.triggered());
        assertEquals(0, result.riskScore());
    }
}
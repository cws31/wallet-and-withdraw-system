package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.auth.repository.AuthenticationAttemptRepository;
import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SuspiciousAccountActivityRuleTest {

    private AuthenticationAttemptRepository authenticationAttemptRepository;

    private FraudRuleProperties properties;

    private SuspiciousAccountActivityRule rule;

    private LocalDateTime evaluationTime;

    @BeforeEach
    void setUp() {

        authenticationAttemptRepository = mock(AuthenticationAttemptRepository.class);

        properties = new FraudRuleProperties();

        rule = new SuspiciousAccountActivityRule(
                authenticationAttemptRepository,
                properties);

        evaluationTime = LocalDateTime.of(
                2026,
                10,
                6,
                22,
                0);
    }

    @Test
    void shouldNotTriggerWhenAttemptsAreAtBaseline() {

        when(authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        eq(1L),
                        any(LocalDateTime.class)))
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
    void shouldCalculateDynamicScoreForThreeAttempts() {

        when(authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        eq(1L),
                        any(LocalDateTime.class)))
                .thenReturn(3L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertTrue(result.triggered());
        assertEquals(
                1,
                result.riskScore());
    }

    @Test
    void shouldCalculateDynamicScoreForFiveAttempts() {

        when(authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        eq(1L),
                        any(LocalDateTime.class)))
                .thenReturn(5L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertTrue(result.triggered());
        assertEquals(
                10,
                result.riskScore());
    }

    @Test
    void shouldCalculateDynamicScoreForSevenAttempts() {

        when(authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        eq(1L),
                        any(LocalDateTime.class)))
                .thenReturn(7L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertTrue(result.triggered());
        assertEquals(
                28,
                result.riskScore());
    }

    @Test
    void shouldCapScoreAtMaximum() {

        when(authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        eq(1L),
                        any(LocalDateTime.class)))
                .thenReturn(20L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertTrue(result.triggered());
        assertEquals(
                40,
                result.riskScore());
    }

    @Test
    void shouldUseConfiguredValuesDynamically() {

        FraudRuleProperties.SuspiciousAccountActivity config = properties.getSuspiciousAccountActivity();

        config.setBaseline(1);
        config.setMaxExpected(5);
        config.setMaxScore(50);

        when(authenticationAttemptRepository
                .countByUserIdAndSuccessFalseAndCreatedAtAfter(
                        eq(1L),
                        any(LocalDateTime.class)))
                .thenReturn(3L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertTrue(result.triggered());
        assertEquals(
                13,
                result.riskScore());
    }
}
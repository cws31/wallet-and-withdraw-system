package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.config.FraudRuleProperties;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnusualWalletActivityRuleTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private FraudRuleProperties properties;

    private UnusualWalletActivityRule rule;

    private final LocalDateTime evaluationTime = LocalDateTime.of(
            2026,
            10,
            5,
            12,
            0);

    @BeforeEach
    void setUp() {

        properties = new FraudRuleProperties();

        rule = new UnusualWalletActivityRule(
                walletTransactionRepository,
                properties);
    }

    @Test
    void shouldAllowNormalWalletActivity() {

        when(
                walletTransactionRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                TransactionStatus.COMPLETED,
                                evaluationTime.minusMinutes(15)))
                .thenReturn(4L);

        RiskRuleResult result = rule.evaluate(
                1L,
                evaluationTime);

        assertFalse(result.triggered());

        assertEquals(
                0,
                result.riskScore());
    }

    @Test
    void shouldCalculateProgressivelyForUnusualActivity() {

        when(
                walletTransactionRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                TransactionStatus.COMPLETED,
                                evaluationTime.minusMinutes(15)))
                .thenReturn(
                        5L,
                        6L,
                        8L);

        assertEquals(
                0,
                rule.evaluate(
                        1L,
                        evaluationTime)
                        .riskScore());

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
    }

    @Test
    void shouldReachConfiguredMaximumAtOrAboveMaximumExpectedActivity() {

        when(
                walletTransactionRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                TransactionStatus.COMPLETED,
                                evaluationTime.minusMinutes(15)))
                .thenReturn(
                        16L,
                        30L);

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
                walletTransactionRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                TransactionStatus.COMPLETED,
                                evaluationTime.minusMinutes(15)))
                .thenReturn(
                        8L,
                        8L);

        properties
                .getUnusualWalletActivity()
                .setMaxScore(35);

        int scoreWithThirtyFiveMax = rule.evaluate(
                1L,
                evaluationTime)
                .riskScore();

        properties
                .getUnusualWalletActivity()
                .setMaxScore(70);

        int scoreWithSeventyMax = rule.evaluate(
                1L,
                evaluationTime)
                .riskScore();

        assertEquals(
                4,
                scoreWithThirtyFiveMax);

        assertEquals(
                8,
                scoreWithSeventyMax);

        assertNotEquals(
                scoreWithThirtyFiveMax,
                scoreWithSeventyMax);
    }

    @Test
    void shouldRespectConfiguredWindow() {

        properties
                .getUnusualWalletActivity()
                .setWindowMinutes(5);

        when(
                walletTransactionRepository
                        .countByUserIdAndStatusAndCreatedAtAfter(
                                1L,
                                TransactionStatus.COMPLETED,
                                evaluationTime.minusMinutes(5)))
                .thenReturn(8L);

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
                UnusualWalletActivityRule
                        .calculateSeverity(
                                4,
                                4,
                                16));

        assertEquals(
                0.0,
                UnusualWalletActivityRule
                        .calculateSeverity(
                                0,
                                4,
                                16));

        assertEquals(
                1.0,
                UnusualWalletActivityRule
                        .calculateSeverity(
                                16,
                                4,
                                16));

        assertEquals(
                1.0,
                UnusualWalletActivityRule
                        .calculateSeverity(
                                50,
                                4,
                                16));
    }
}
package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuspiciousPayoutRuleTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private PayoutOption payoutOption;

    private SuspiciousPayoutRule rule;

    private User user;

    private Withdrawal withdrawal;

    @BeforeEach
    void setUp() {

        rule = new SuspiciousPayoutRule(
                withdrawalRepository);

        user = new User();
        user.setId(1L);

        withdrawal = new Withdrawal();

        withdrawal.setPayoutOption(payoutOption);
        withdrawal.setCurrencyAmount(
                new BigDecimal("100"));

        when(payoutOption.getId())
                .thenReturn(10L);
    }

    @Test
    void shouldNotTriggerForFirstSimilarRequest() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                                eq(1L),
                                eq(10L),
                                eq(new BigDecimal("100")),
                                any(LocalDateTime.class)))
                .thenReturn(0L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertFalse(result.isTriggered());

        assertEquals(
                "SUSPICIOUS_PAYOUT_REQUESTS",
                result.getRuleCode());

        assertEquals(
                0,
                result.getRiskScore());

        assertNull(result.getExplanation());
    }

    @Test
    void shouldReturnTenPointsForSecondSimilarRequest() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                                eq(1L),
                                eq(10L),
                                eq(new BigDecimal("100")),
                                any(LocalDateTime.class)))
                .thenReturn(1L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                "SUSPICIOUS_PAYOUT_REQUESTS",
                result.getRuleCode());

        assertEquals(
                10,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("2 similar payout requests"));
    }

    @Test
    void shouldReturnThirtyPointsForFourthSimilarRequest() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                                eq(1L),
                                eq(10L),
                                eq(new BigDecimal("100")),
                                any(LocalDateTime.class)))
                .thenReturn(3L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                30,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("4 similar payout requests"));
    }

    @Test
    void shouldReturnMaximumScoreForSevenOrMoreSimilarRequests() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                                eq(1L),
                                eq(10L),
                                eq(new BigDecimal("100")),
                                any(LocalDateTime.class)))
                .thenReturn(6L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                40,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("7 similar payout requests"));
    }
}
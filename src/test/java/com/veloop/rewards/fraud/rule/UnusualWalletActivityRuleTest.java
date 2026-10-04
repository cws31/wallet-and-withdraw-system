package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnusualWalletActivityRuleTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private UnusualWalletActivityRule rule;

    private User user;

    private Withdrawal withdrawal;

    @BeforeEach
    void setUp() {

        rule = new UnusualWalletActivityRule(
                walletTransactionRepository);

        user = new User();
        user.setId(1L);

        withdrawal = new Withdrawal();
    }

    @Test
    void shouldReturnTenPointsForFiveTransactions() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(5L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                "UNUSUAL_WALLET_ACTIVITY",
                result.getRuleCode());

        assertEquals(
                10,
                result.getRiskScore());

        assertNotNull(result.getExplanation());

        assertTrue(
                result.getExplanation()
                        .contains("5 wallet transactions"));
    }

    @Test
    void shouldReturnTwentyPointsForTenTransactions() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(10L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                "UNUSUAL_WALLET_ACTIVITY",
                result.getRuleCode());

        assertEquals(
                20,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("10 wallet transactions"));
    }

    @Test
    void shouldNotTriggerWhenFewerThanFiveTransactionsExist() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(4L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertFalse(result.isTriggered());

        assertEquals(
                "UNUSUAL_WALLET_ACTIVITY",
                result.getRuleCode());

        assertEquals(
                0,
                result.getRiskScore());

        assertNull(result.getExplanation());
    }

    @Test
    void shouldReturnMaximumScoreForSixteenOrMoreTransactions() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(16L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                "UNUSUAL_WALLET_ACTIVITY",
                result.getRuleCode());

        assertEquals(
                30,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("16 wallet transactions"));
    }
}
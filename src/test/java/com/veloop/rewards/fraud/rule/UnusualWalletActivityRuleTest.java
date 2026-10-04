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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void shouldTriggerWhenTenOrMoreTransactionsExist() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(10L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());

        assertEquals(
                "User has unusually high wallet "
                        + "transaction activity within "
                        + "a short time window",
                result.get());

        assertEquals(
                "UNUSUAL_WALLET_ACTIVITY",
                rule.getRuleCode());

        assertEquals(
                20,
                rule.getRiskScore());
    }

    @Test
    void shouldNotTriggerWhenFewerThanTenTransactionsExist() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(9L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotTriggerWhenThereAreNoTransactions() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(0L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTriggerAtExactThreshold() {

        when(
                walletTransactionRepository
                        .countByUserIdAndCreatedAtAfter(
                                eq(1L),
                                any(LocalDateTime.class)))
                .thenReturn(10L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());
    }
}
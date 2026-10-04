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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuspiciousPayoutRuleTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;

    private SuspiciousPayoutRule rule;

    private User user;

    private Withdrawal withdrawal;

    private PayoutOption payoutOption;

    @BeforeEach
    void setUp() {

        rule = new SuspiciousPayoutRule(
                withdrawalRepository);

        user = new User();
        user.setId(1L);

        payoutOption = new PayoutOption();
        payoutOption.setId(10L);

        withdrawal = new Withdrawal();

        withdrawal.setPayoutOption(payoutOption);
        withdrawal.setCurrencyAmount(
                new BigDecimal("100.00"));
    }

    @Test
    void shouldTriggerWhenFourSimilarRequestsExist() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                                eq(1L),
                                eq(10L),
                                eq(new BigDecimal("100.00")),
                                any(LocalDateTime.class)))
                .thenReturn(4L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());

        assertEquals(
                "User has repeatedly requested the same "
                        + "payout option and amount within "
                        + "a short time window",
                result.get());

        assertEquals(
                "SUSPICIOUS_PAYOUT_REQUESTS",
                rule.getRuleCode());

        assertEquals(
                30,
                rule.getRiskScore());
    }

    @Test
    void shouldNotTriggerWhenFewerThanFourSimilarRequestsExist() {

        when(
                withdrawalRepository
                        .countByUserIdAndPayoutOptionIdAndCurrencyAmountAndCreatedAtAfter(
                                eq(1L),
                                eq(10L),
                                eq(new BigDecimal("100.00")),
                                any(LocalDateTime.class)))
                .thenReturn(3L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotTriggerWhenPayoutOptionIsMissing() {

        withdrawal.setPayoutOption(null);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());

        verifyNoInteractions(withdrawalRepository);
    }

    @Test
    void shouldNotTriggerWhenCurrencyAmountIsMissing() {

        withdrawal.setCurrencyAmount(null);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());

        verifyNoInteractions(withdrawalRepository);
    }
}
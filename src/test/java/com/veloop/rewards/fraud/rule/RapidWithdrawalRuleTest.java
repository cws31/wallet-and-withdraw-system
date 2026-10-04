package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.fraud.repository.FraudRiskEventRepository;
import com.veloop.rewards.user.entity.User;
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
class RapidWithdrawalRuleTest {

    @Mock
    private FraudRiskEventRepository fraudRiskEventRepository;

    private RapidWithdrawalRule rule;

    private User user;

    private Withdrawal withdrawal;

    @BeforeEach
    void setUp() {

        rule = new RapidWithdrawalRule(
                fraudRiskEventRepository);

        user = new User();

        withdrawal = new Withdrawal();
    }

    @Test
    void shouldTriggerWhenThreeOrMoreRecentRiskEventsExist() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                eq(user),
                                any(LocalDateTime.class)))
                .thenReturn(3L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());

        assertEquals(
                "User has multiple withdrawal risk events "
                        + "within a short time window",
                result.get());

        assertEquals(
                "RAPID_WITHDRAWAL",
                rule.getRuleCode());

        assertEquals(
                25,
                rule.getRiskScore());
    }

    @Test
    void shouldNotTriggerWhenFewerThanThreeRecentRiskEventsExist() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                eq(user),
                                any(LocalDateTime.class)))
                .thenReturn(2L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotTriggerWhenNoRecentRiskEventsExist() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                eq(user),
                                any(LocalDateTime.class)))
                .thenReturn(0L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTriggerAtExactThreshold() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                eq(user),
                                any(LocalDateTime.class)))
                .thenReturn(3L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());
    }
}
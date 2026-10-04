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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepeatedWithdrawalRuleTest {

    @Mock
    private FraudRiskEventRepository fraudRiskEventRepository;

    @Mock
    private User user;

    @Mock
    private Withdrawal withdrawal;

    private RepeatedWithdrawalRule rule;

    @BeforeEach
    void setUp() {
        rule = new RepeatedWithdrawalRule(
                fraudRiskEventRepository);
    }

    @Test
    void shouldNotTriggerForFirstWithdrawal() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                any(User.class),
                                any(LocalDateTime.class)))
                .thenReturn(0L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertFalse(result.isTriggered());

        assertEquals(
                "REPEATED_WITHDRAWAL",
                result.getRuleCode());

        assertEquals(
                0,
                result.getRiskScore());

        assertNull(result.getExplanation());
    }

    @Test
    void shouldNotTriggerForSecondWithdrawal() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                any(User.class),
                                any(LocalDateTime.class)))
                .thenReturn(1L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertFalse(result.isTriggered());

        assertEquals(
                0,
                result.getRiskScore());
    }

    @Test
    void shouldReturnTenPointsForThirdWithdrawal() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                any(User.class),
                                any(LocalDateTime.class)))
                .thenReturn(2L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                "REPEATED_WITHDRAWAL",
                result.getRuleCode());

        assertEquals(
                10,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("3 withdrawal requests"));
    }

    @Test
    void shouldIncreaseScoreForHighWithdrawalFrequency() {

        when(
                fraudRiskEventRepository
                        .countByUserAndCreatedAtAfter(
                                any(User.class),
                                any(LocalDateTime.class)))
                .thenReturn(7L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                30,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("8 withdrawal requests"));
    }
}
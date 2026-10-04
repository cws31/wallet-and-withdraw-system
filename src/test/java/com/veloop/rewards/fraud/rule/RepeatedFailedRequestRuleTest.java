package com.veloop.rewards.fraud.rule;

import com.veloop.rewards.audit.repository.AuditLogRepository;
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
class RepeatedFailedRequestRuleTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private User user;

    @Mock
    private Withdrawal withdrawal;

    private RepeatedFailedRequestRule rule;

    @BeforeEach
    void setUp() {
        rule = new RepeatedFailedRequestRule(
                auditLogRepository);

        when(user.getId()).thenReturn(1L);
    }

    @Test
    void shouldNotTriggerWhenFailedRequestsAreBelowThreshold() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                1L,
                                "WITHDRAWAL_REQUEST_FAILED",
                                any(LocalDateTime.class)))
                .thenReturn(2L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertFalse(result.isTriggered());

        assertEquals(
                "REPEATED_FAILED_REQUESTS",
                result.getRuleCode());

        assertEquals(
                0,
                result.getRiskScore());

        assertNull(result.getExplanation());
    }

    @Test
    void shouldReturnTenPointsForThreeFailedRequests() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                1L,
                                "WITHDRAWAL_REQUEST_FAILED",
                                any(LocalDateTime.class)))
                .thenReturn(3L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                "REPEATED_FAILED_REQUESTS",
                result.getRuleCode());

        assertEquals(
                10,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("3 failed withdrawal requests"));
    }

    @Test
    void shouldReturnTwentyPointsForFiveFailedRequests() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                1L,
                                "WITHDRAWAL_REQUEST_FAILED",
                                any(LocalDateTime.class)))
                .thenReturn(5L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                20,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("5 failed withdrawal requests"));
    }

    @Test
    void shouldReturnMaximumScoreForEightOrMoreFailedRequests() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                1L,
                                "WITHDRAWAL_REQUEST_FAILED",
                                any(LocalDateTime.class)))
                .thenReturn(8L);

        RiskRuleResult result = rule.evaluate(user, withdrawal);

        assertTrue(result.isTriggered());

        assertEquals(
                30,
                result.getRiskScore());

        assertTrue(
                result.getExplanation()
                        .contains("8 failed withdrawal requests"));
    }
}
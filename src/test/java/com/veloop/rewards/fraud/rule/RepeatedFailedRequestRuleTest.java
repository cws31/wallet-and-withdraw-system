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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepeatedFailedRequestRuleTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private RepeatedFailedRequestRule rule;

    private User user;

    private Withdrawal withdrawal;

    @BeforeEach
    void setUp() {

        rule = new RepeatedFailedRequestRule(
                auditLogRepository);

        user = new User();
        user.setId(1L);

        withdrawal = new Withdrawal();
    }

    @Test
    void shouldTriggerWhenFiveFailedRequestsExist() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                eq(1L),
                                eq("WITHDRAWAL_REQUEST_FAILED"),
                                any(LocalDateTime.class)))
                .thenReturn(5L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());

        assertEquals(
                "User has repeated failed withdrawal "
                        + "requests within a short time window",
                result.get());

        assertEquals(
                "REPEATED_FAILED_REQUESTS",
                rule.getRuleCode());

        assertEquals(
                15,
                rule.getRiskScore());
    }

    @Test
    void shouldNotTriggerWhenFewerThanFiveFailedRequestsExist() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                eq(1L),
                                eq("WITHDRAWAL_REQUEST_FAILED"),
                                any(LocalDateTime.class)))
                .thenReturn(4L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotTriggerWhenThereAreNoFailedRequests() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                eq(1L),
                                eq("WITHDRAWAL_REQUEST_FAILED"),
                                any(LocalDateTime.class)))
                .thenReturn(0L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldTriggerAtExactThreshold() {

        when(
                auditLogRepository
                        .countByTargetUserIdAndActionAndCreatedAtAfter(
                                eq(1L),
                                eq("WITHDRAWAL_REQUEST_FAILED"),
                                any(LocalDateTime.class)))
                .thenReturn(5L);

        Optional<String> result = rule.evaluate(user, withdrawal);

        assertTrue(result.isPresent());
    }
}
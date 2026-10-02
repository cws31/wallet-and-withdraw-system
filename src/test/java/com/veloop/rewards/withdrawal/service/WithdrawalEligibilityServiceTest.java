package com.veloop.rewards.withdrawal.service;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WithdrawalEligibilityServiceTest {

    private WithdrawalEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new WithdrawalEligibilityService();
    }

    @Test
    void shouldAllowVerifiedActiveUser() {
        User user = User.builder()
                .accountStatus("ACTIVE")
                .verified(true)
                .build();

        assertDoesNotThrow(() -> service.validate(user));
    }

    @Test
    void shouldRejectNullUser() {
        assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> service.validate(null));
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = User.builder()
                .accountStatus("INACTIVE")
                .verified(true)
                .build();

        assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> service.validate(user));
    }

    @Test
    void shouldRejectSuspendedUser() {
        User user = User.builder()
                .accountStatus("SUSPENDED")
                .verified(true)
                .build();

        assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> service.validate(user));
    }

    @Test
    void shouldRejectUnverifiedUser() {
        User user = User.builder()
                .accountStatus("ACTIVE")
                .verified(false)
                .build();

        assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> service.validate(user));
    }

    @Test
    void shouldAcceptActiveStatusCaseInsensitively() {
        User user = User.builder()
                .accountStatus("active")
                .verified(true)
                .build();

        assertDoesNotThrow(() -> service.validate(user));
    }
}
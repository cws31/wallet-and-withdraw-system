package com.veloop.rewards.payout.validation;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayoutDetailValidatorTest {

    private PayoutDetailValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PayoutDetailValidator();
    }

    @Test
    void shouldAcceptValidUpiId() {

        assertDoesNotThrow(() -> validator.validate(
                "UPI",
                "vijay@upi"));
    }

    @Test
    void shouldAcceptValidUpiIdWithCommonProvider() {

        assertDoesNotThrow(() -> validator.validate(
                "UPI",
                "9876543210@ybl"));
    }

    @Test
    void shouldRejectInvalidUpiIdWithoutAtSymbol() {

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        "UPI",
                        "vijayupi"));

        assertEquals(
                "Invalid UPI ID",
                exception.getMessage());
    }

    @Test
    void shouldRejectInvalidUpiIdWithMissingProvider() {

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        "UPI",
                        "vijay@"));

        assertEquals(
                "Invalid UPI ID",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankPayoutDetails() {

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        "UPI",
                        "   "));

        assertEquals(
                "Payout details are required",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullPayoutDetails() {

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        "UPI",
                        null));

        assertEquals(
                "Payout details are required",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankPayoutMethod() {

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        "   ",
                        "vijay@upi"));

        assertEquals(
                "Payout method is required",
                exception.getMessage());
    }

    @Test
    void shouldAcceptNonBlankDetailsForOtherMethodsForNow() {

        assertDoesNotThrow(() -> validator.validate(
                "AMAZON_GIFT_CARD",
                "recipient-details"));
    }

    @Test
    void shouldHandleLowercaseUpiMethodCode() {

        assertDoesNotThrow(() -> validator.validate(
                "upi",
                "vijay@upi"));
    }
}

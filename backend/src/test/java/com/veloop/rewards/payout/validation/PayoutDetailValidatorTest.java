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
        void shouldAcceptValidUpiIdWithSurroundingWhitespace() {

                assertDoesNotThrow(() -> validator.validate(
                                "  UPI  ",
                                "  vijay@upi  "));
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
        void shouldAcceptValidEmailForAmazonGiftCard() {

                assertDoesNotThrow(() -> validator.validate(
                                "AMAZON_GIFT_CARD",
                                "user@example.com"));
        }

        @Test
        void shouldAcceptValidEmailForGooglePlayGiftCard() {

                assertDoesNotThrow(() -> validator.validate(
                                "GOOGLE_PLAY_GIFT_CARD",
                                "user@example.com"));
        }

        @Test
        void shouldHandleLowercaseUpiMethodCode() {

                assertDoesNotThrow(() -> validator.validate(
                                "upi",

                                "vijay@upi"));
        }

        @Test
        void shouldRejectInvalidAmazonGiftCardEmail() {

                InvalidWithdrawalRequestException exception = assertThrows(
                                InvalidWithdrawalRequestException.class,
                                () -> validator.validate(
                                                "AMAZON_GIFT_CARD",
                                                "invalid-email"));

                assertEquals(
                                "Invalid email address for Amazon Gift Card",
                                exception.getMessage());
        }

        @Test
        void shouldRejectInvalidGooglePlayGiftCardEmail() {

                InvalidWithdrawalRequestException exception = assertThrows(
                                InvalidWithdrawalRequestException.class,
                                () -> validator.validate(
                                                "GOOGLE_PLAY_GIFT_CARD",
                                                "invalid-email"));

                assertEquals(
                                "Invalid email address for Google Play Gift Card",
                                exception.getMessage());
        }
}

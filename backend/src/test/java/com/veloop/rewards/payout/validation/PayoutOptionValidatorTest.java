package com.veloop.rewards.payout.validation;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PayoutOptionValidatorTest {

    private PayoutOptionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PayoutOptionValidator();
    }

    @Test
    void shouldAcceptValidActiveInrPayoutOption() {

        PayoutMethod method = createMethod(1L, true);
        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "2400",
                "10");

        assertDoesNotThrow(() -> validator.validate(method, option));
    }

    @Test
    void shouldRejectNullPayoutMethod() {

        PayoutOption option = createOption(
                10L,
                null,
                true,
                "INR",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(null, option));

        assertEquals(
                "Payout method is required",
                exception.getMessage());
    }

    @Test
    void shouldRejectInactivePayoutMethod() {

        PayoutMethod method = createMethod(1L, false);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(method, option));

        assertEquals(
                "Selected payout method is inactive",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullPayoutOption() {

        PayoutMethod method = createMethod(1L, true);

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(method, null));

        assertEquals(
                "Payout option is required",
                exception.getMessage());
    }

    @Test
    void shouldRejectInactivePayoutOption() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                false,
                "INR",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(method, option));

        assertEquals(
                "Selected payout option is inactive",
                exception.getMessage());
    }

    @Test
    void shouldRejectOptionBelongingToDifferentMethod() {

        PayoutMethod selectedMethod = createMethod(1L, true);

        PayoutMethod differentMethod = createMethod(2L, true);

        PayoutOption option = createOption(
                10L,
                differentMethod,
                true,
                "INR",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        selectedMethod,
                        option));

        assertEquals(
                "Payout option does not belong to selected payout method",
                exception.getMessage());
    }

    @Test
    void shouldRejectOptionWithNullMethod() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                null,
                true,
                "INR",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout option does not belong to selected payout method",
                exception.getMessage());
    }

    @Test
    void shouldRejectOptionWithNullPayoutMethodId() {

        PayoutMethod method = createMethod(null, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout option does not belong to selected payout method",
                exception.getMessage());
    }

    @Test
    void shouldRejectZeroPayoutAmount() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "2400",
                "0");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout amount must be greater than zero",
                exception.getMessage());
    }

    @Test
    void shouldRejectNegativePayoutAmount() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "2400",
                "-10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout amount must be greater than zero",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullPayoutAmount() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "2400",
                null);

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout amount must be greater than zero",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankCurrency() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "   ",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout currency is required",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullCurrency() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                null,
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Payout currency is required",
                exception.getMessage());
    }

    @Test
    void shouldAcceptCurrencyCaseInsensitively() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "inr",
                "2400",
                "10");

        assertDoesNotThrow(() -> validator.validate(method, option));
    }

    @Test
    void shouldRejectUnsupportedCurrency() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "USD",
                "2400",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Unsupported payout currency: USD",
                exception.getMessage());
    }

    @Test
    void shouldRejectZeroCurrencyAmount() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "0",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Required currency amount must be greater than zero",
                exception.getMessage());
    }

    @Test
    void shouldRejectNegativeCurrencyAmount() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                "-10",
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Required currency amount must be greater than zero",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullCurrencyAmount() {

        PayoutMethod method = createMethod(1L, true);

        PayoutOption option = createOption(
                10L,
                method,
                true,
                "INR",
                null,
                "10");

        InvalidWithdrawalRequestException exception = assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> validator.validate(
                        method,
                        option));

        assertEquals(
                "Required currency amount must be greater than zero",
                exception.getMessage());
    }

    private PayoutMethod createMethod(
            Long id,
            boolean active) {

        PayoutMethod method = new PayoutMethod();

        method.setId(id);
        method.setCode("UPI");
        method.setName("UPI");
        method.setActive(active);

        return method;
    }

    private PayoutOption createOption(
            Long id,
            PayoutMethod method,
            boolean active,
            String currency,
            String currencyAmount,
            String payoutAmount) {

        PayoutOption option = new PayoutOption();

        option.setId(id);
        option.setMethod(method);
        option.setActive(active);
        option.setCurrency(currency);

        if (currencyAmount != null) {
            option.setCurrencyAmount(
                    new BigDecimal(currencyAmount));
        }

        if (payoutAmount != null) {
            option.setPayoutAmount(
                    new BigDecimal(payoutAmount));
        }

        return option;
    }
}

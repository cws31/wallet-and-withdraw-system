package com.veloop.rewards.payoutprocessing.provider;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FakePayoutProviderTest {

    @Test
    void shouldReturnApprovedResult() {

        FakePayoutProvider provider = new FakePayoutProvider(
                "UPI",
                PayoutProviderResult.approved(
                        "FAKE-PROVIDER-001"));

        PayoutProviderRequest request = new PayoutProviderRequest(
                "WD-FAKE-001",
                "UPI",
                "INR",
                new BigDecimal("2400"),
                new BigDecimal("10"),
                "9876543210@ybl");

        PayoutProviderResult result = provider.process(request);

        assertNotNull(result);

        assertEquals(
                PayoutProviderStatus.APPROVED,
                result.status());

        assertEquals(
                "FAKE-PROVIDER-001",
                result.providerReference());

        assertNull(
                result.failureReason());

        assertEquals(
                "UPI",
                provider.getMethodCode());
    }

    @Test
    void shouldReturnRejectedResult() {

        FakePayoutProvider provider = new FakePayoutProvider(
                "UPI",
                PayoutProviderResult.rejected(
                        "Fake provider rejection"));

        PayoutProviderRequest request = new PayoutProviderRequest(
                "WD-FAKE-002",
                "UPI",
                "INR",
                new BigDecimal("2400"),
                new BigDecimal("10"),
                "9876543210@ybl");

        PayoutProviderResult result = provider.process(request);

        assertNotNull(result);

        assertEquals(
                PayoutProviderStatus.REJECTED,
                result.status());

        assertNull(
                result.providerReference());

        assertEquals(
                "Fake provider rejection",
                result.failureReason());
    }

    @Test
    void shouldAllowProviderResultToBeChangedForLaterAttempt() {

        FakePayoutProvider provider = new FakePayoutProvider(
                "UPI",
                PayoutProviderResult.rejected(
                        "Temporary provider failure"));

        PayoutProviderRequest request = new PayoutProviderRequest(
                "WD-FAKE-003",
                "UPI",
                "INR",
                new BigDecimal("2400"),
                new BigDecimal("10"),
                "9876543210@ybl");

        PayoutProviderResult firstResult = provider.process(request);

        assertEquals(
                PayoutProviderStatus.REJECTED,
                firstResult.status());

        provider.setNextResult(
                PayoutProviderResult.approved(
                        "FAKE-PROVIDER-003"));

        PayoutProviderResult secondResult = provider.process(request);

        assertEquals(
                PayoutProviderStatus.APPROVED,
                secondResult.status());

        assertEquals(
                "FAKE-PROVIDER-003",
                secondResult.providerReference());
    }

    @Test
    void shouldRejectNullRequest() {

        FakePayoutProvider provider = new FakePayoutProvider(
                "UPI",
                PayoutProviderResult.approved(
                        "FAKE-PROVIDER-004"));

        assertThrows(
                IllegalArgumentException.class,
                () -> provider.process(null));
    }
}
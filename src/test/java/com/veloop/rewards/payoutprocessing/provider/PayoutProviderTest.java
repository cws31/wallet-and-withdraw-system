package com.veloop.rewards.payoutprocessing.provider;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayoutProviderTest {

    @Test
    void shouldProcessApprovedProviderResponse() {

        PayoutProvider provider = mock(PayoutProvider.class);

        when(provider.getMethodCode())
                .thenReturn("UPI");

        PayoutProviderRequest request = new PayoutProviderRequest(
                "WD-TEST-001",
                "UPI",
                "INR",
                new BigDecimal("2400"),
                new BigDecimal("10"),
                "9876543210@ybl");

        when(provider.process(request))
                .thenReturn(
                        PayoutProviderResult.approved(
                                "PROVIDER-001"));

        assertEquals(
                "UPI",
                provider.getMethodCode());

        PayoutProviderResult result = provider.process(request);

        assertNotNull(result);

        assertEquals(
                PayoutProviderStatus.APPROVED,
                result.status());

        assertEquals(
                "PROVIDER-001",
                result.providerReference());

        assertNull(
                result.failureReason());

        verify(provider, times(1))
                .process(request);
    }

    @Test
    void shouldProcessRejectedProviderResponse() {

        PayoutProvider provider = mock(PayoutProvider.class);

        when(provider.getMethodCode())
                .thenReturn("UPI");

        PayoutProviderRequest request = new PayoutProviderRequest(
                "WD-TEST-002",
                "UPI",
                "INR",
                new BigDecimal("2400"),
                new BigDecimal("10"),
                "9876543210@ybl");

        when(provider.process(request))
                .thenReturn(
                        PayoutProviderResult.rejected(
                                "Provider rejected payout"));

        assertEquals(
                "UPI",
                provider.getMethodCode());

        PayoutProviderResult result = provider.process(request);

        assertNotNull(result);

        assertEquals(
                PayoutProviderStatus.REJECTED,
                result.status());

        assertNull(
                result.providerReference());

        assertEquals(
                "Provider rejected payout",
                result.failureReason());

        verify(provider, times(1))
                .process(request);
    }
}
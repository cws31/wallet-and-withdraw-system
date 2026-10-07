package com.veloop.rewards.payoutprocessing.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayoutProviderRegistryTest {

    @Test
    void shouldResolveProviderByMethodCode() {

        PayoutProvider upiProvider = new TestPayoutProvider("UPI");

        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(upiProvider));

        PayoutProvider resolved = registry.getProvider("UPI");

        assertSame(
                upiProvider,
                resolved);
    }

    @Test
    void shouldResolveProviderCaseInsensitively() {

        PayoutProvider upiProvider = new TestPayoutProvider("UPI");

        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(upiProvider));

        PayoutProvider resolved = registry.getProvider("upi");

        assertSame(
                upiProvider,
                resolved);
    }

    @Test
    void shouldTrimMethodCodeBeforeResolution() {

        PayoutProvider upiProvider = new TestPayoutProvider("UPI");

        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(upiProvider));

        PayoutProvider resolved = registry.getProvider("  UPI  ");

        assertSame(
                upiProvider,
                resolved);
    }

    @Test
    void shouldRejectUnknownMethodCode() {

        PayoutProvider upiProvider = new TestPayoutProvider("UPI");

        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(upiProvider));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getProvider(
                        "UNKNOWN_METHOD"));

        assertEquals(
                "No payout provider configured for method: UNKNOWN_METHOD",
                exception.getMessage());
    }

    @Test
    void shouldRejectBlankMethodCode() {

        PayoutProvider upiProvider = new TestPayoutProvider("UPI");

        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(upiProvider));

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.getProvider("   "));
    }

    @Test
    void shouldResolveMultipleProviders() {

        PayoutProvider upiProvider = new TestPayoutProvider("UPI");

        PayoutProvider amazonProvider = new TestPayoutProvider(
                "AMAZON_GIFT_CARD");

        PayoutProvider googleProvider = new TestPayoutProvider(
                "GOOGLE_PLAY_GIFT_CARD");

        PayoutProviderRegistry registry = new PayoutProviderRegistry(
                List.of(
                        upiProvider,
                        amazonProvider,
                        googleProvider));

        assertSame(
                upiProvider,
                registry.getProvider("UPI"));

        assertSame(
                amazonProvider,
                registry.getProvider(
                        "AMAZON_GIFT_CARD"));

        assertSame(
                googleProvider,
                registry.getProvider(
                        "GOOGLE_PLAY_GIFT_CARD"));
    }

    private static class TestPayoutProvider
            implements PayoutProvider {

        private final String methodCode;

        private TestPayoutProvider(
                String methodCode) {

            this.methodCode = methodCode;
        }

        @Override
        public String getMethodCode() {
            return methodCode;
        }

        @Override
        public PayoutProviderResult process(
                PayoutProviderRequest request) {

            return PayoutProviderResult.approved(
                    "TEST-PROVIDER-REFERENCE");
        }
    }
}
package com.veloop.rewards.payoutprocessing.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PayoutProviderRegistry {

    private final Map<String, PayoutProvider> providers;

    public PayoutProviderRegistry(
            List<PayoutProvider> payoutProviders) {

        this.providers = payoutProviders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> normalizeMethodCode(
                                provider.getMethodCode()),
                        Function.identity()));
    }

    public PayoutProvider getProvider(
            String methodCode) {

        String normalizedMethodCode = normalizeMethodCode(methodCode);

        PayoutProvider provider = providers.get(normalizedMethodCode);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "No payout provider configured for method: "
                            + methodCode);
        }

        return provider;
    }

    private String normalizeMethodCode(
            String methodCode) {

        if (methodCode == null
                || methodCode.isBlank()) {

            throw new IllegalArgumentException(
                    "Payout method code must not be blank");
        }

        return methodCode
                .trim()
                .toUpperCase();
    }
}
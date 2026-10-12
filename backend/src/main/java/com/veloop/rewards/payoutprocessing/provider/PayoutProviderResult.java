package com.veloop.rewards.payoutprocessing.provider;

public record PayoutProviderResult(

        PayoutProviderStatus status,

        String providerReference,

        String failureReason

) {

    public static PayoutProviderResult approved(
            String providerReference) {

        return new PayoutProviderResult(
                PayoutProviderStatus.APPROVED,
                providerReference,
                null);
    }

    public static PayoutProviderResult rejected(
            String failureReason) {

        return new PayoutProviderResult(
                PayoutProviderStatus.REJECTED,
                null,
                failureReason);
    }
}
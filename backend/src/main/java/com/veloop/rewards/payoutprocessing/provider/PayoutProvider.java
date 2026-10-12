package com.veloop.rewards.payoutprocessing.provider;

public interface PayoutProvider {

    String getMethodCode();

    PayoutProviderResult process(
            PayoutProviderRequest request);
}
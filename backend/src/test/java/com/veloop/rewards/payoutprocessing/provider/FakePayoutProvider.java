package com.veloop.rewards.payoutprocessing.provider;

public class FakePayoutProvider implements PayoutProvider {

    private final String methodCode;
    private PayoutProviderResult nextResult;

    public FakePayoutProvider(
            String methodCode,
            PayoutProviderResult nextResult) {

        this.methodCode = methodCode;
        this.nextResult = nextResult;
    }

    @Override
    public String getMethodCode() {
        return methodCode;
    }

    @Override
    public PayoutProviderResult process(
            PayoutProviderRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Payout provider request must not be null");
        }

        return nextResult;
    }

    public void setNextResult(
            PayoutProviderResult nextResult) {

        this.nextResult = nextResult;
    }
}
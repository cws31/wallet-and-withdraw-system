package com.veloop.rewards.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ObservabilityMetrics {

    private final Counter walletCreditSuccess;
    private final Counter walletCreditFailure;
    private final Counter walletDebitSuccess;
    private final Counter walletDebitFailure;

    private final Counter withdrawalCreated;
    private final Counter withdrawalProcessing;
    private final Counter withdrawalApproved;
    private final Counter withdrawalRejected;
    private final Counter withdrawalCancelled;

    private final Counter rateLimitBlocked;

    private final Counter reconciliationSuccess;
    private final Counter reconciliationFailure;

    private final Counter applicationError;

    public ObservabilityMetrics(MeterRegistry meterRegistry) {

        this.walletCreditSuccess = counter(
                meterRegistry,
                "wallet.credit.success");

        this.walletCreditFailure = counter(
                meterRegistry,
                "wallet.credit.failure");

        this.walletDebitSuccess = counter(
                meterRegistry,
                "wallet.debit.success");

        this.walletDebitFailure = counter(
                meterRegistry,
                "wallet.debit.failure");

        this.withdrawalCreated = counter(
                meterRegistry,
                "withdrawal.created");

        this.withdrawalProcessing = counter(
                meterRegistry,
                "withdrawal.processing");

        this.withdrawalApproved = counter(
                meterRegistry,
                "withdrawal.approved");

        this.withdrawalRejected = counter(
                meterRegistry,
                "withdrawal.rejected");

        this.withdrawalCancelled = counter(
                meterRegistry,
                "withdrawal.cancelled");

        this.rateLimitBlocked = counter(
                meterRegistry,
                "rate_limit.blocked");

        this.reconciliationSuccess = counter(
                meterRegistry,
                "reconciliation.success");

        this.reconciliationFailure = counter(
                meterRegistry,
                "reconciliation.failure");

        this.applicationError = counter(
                meterRegistry,
                "application.error");
    }

    public void recordWalletCreditSuccess() {
        walletCreditSuccess.increment();
    }

    public void recordWalletCreditFailure() {
        walletCreditFailure.increment();
    }

    public void recordWalletDebitSuccess() {
        walletDebitSuccess.increment();
    }

    public void recordWalletDebitFailure() {
        walletDebitFailure.increment();
    }

    public void recordWithdrawalCreated() {
        withdrawalCreated.increment();
    }

    public void recordWithdrawalProcessing() {
        withdrawalProcessing.increment();
    }

    public void recordWithdrawalApproved() {
        withdrawalApproved.increment();
    }

    public void recordWithdrawalRejected() {
        withdrawalRejected.increment();
    }

    public void recordWithdrawalCancelled() {
        withdrawalCancelled.increment();
    }

    public void recordRateLimitBlocked() {
        rateLimitBlocked.increment();
    }

    public void recordReconciliationSuccess() {
        reconciliationSuccess.increment();
    }

    public void recordReconciliationFailure() {
        reconciliationFailure.increment();
    }

    public void recordApplicationError() {
        applicationError.increment();
    }

    private Counter counter(
            MeterRegistry meterRegistry,
            String name) {

        return Counter.builder(name)
                .description("Veloop rewards application metric: " + name)
                .register(meterRegistry);
    }
}

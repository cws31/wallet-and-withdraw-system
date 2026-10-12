
package com.veloop.rewards.observability.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.veloop.rewards.observability.ObservabilityMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilityMetricsTest {

    private MeterRegistry meterRegistry;
    private ObservabilityMetrics observabilityMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        observabilityMetrics = new ObservabilityMetrics(meterRegistry);
    }

    @Test
    void shouldRecordWalletCreditSuccess() {
        observabilityMetrics.recordWalletCreditSuccess();

        assertEquals(
                1.0,
                meterRegistry.get("wallet.credit.success")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWalletCreditFailure() {
        observabilityMetrics.recordWalletCreditFailure();

        assertEquals(
                1.0,
                meterRegistry.get("wallet.credit.failure")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWalletDebitSuccess() {
        observabilityMetrics.recordWalletDebitSuccess();

        assertEquals(
                1.0,
                meterRegistry.get("wallet.debit.success")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWalletDebitFailure() {
        observabilityMetrics.recordWalletDebitFailure();

        assertEquals(
                1.0,
                meterRegistry.get("wallet.debit.failure")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWithdrawalCreated() {
        observabilityMetrics.recordWithdrawalCreated();

        assertEquals(
                1.0,
                meterRegistry.get("withdrawal.created")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWithdrawalProcessing() {
        observabilityMetrics.recordWithdrawalProcessing();

        assertEquals(
                1.0,
                meterRegistry.get("withdrawal.processing")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWithdrawalApproved() {
        observabilityMetrics.recordWithdrawalApproved();

        assertEquals(
                1.0,
                meterRegistry.get("withdrawal.approved")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWithdrawalRejected() {
        observabilityMetrics.recordWithdrawalRejected();

        assertEquals(
                1.0,
                meterRegistry.get("withdrawal.rejected")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordWithdrawalCancelled() {
        observabilityMetrics.recordWithdrawalCancelled();

        assertEquals(
                1.0,
                meterRegistry.get("withdrawal.cancelled")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordRateLimitBlocked() {
        observabilityMetrics.recordRateLimitBlocked();

        assertEquals(
                1.0,
                meterRegistry.get("rate_limit.blocked")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordReconciliationSuccess() {
        observabilityMetrics.recordReconciliationSuccess();

        assertEquals(
                1.0,
                meterRegistry.get("reconciliation.success")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordReconciliationFailure() {
        observabilityMetrics.recordReconciliationFailure();

        assertEquals(
                1.0,
                meterRegistry.get("reconciliation.failure")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordApplicationError() {
        observabilityMetrics.recordApplicationError();

        assertEquals(
                1.0,
                meterRegistry.get("application.error")
                        .counter()
                        .count());
    }

    @Test
    void shouldAccumulateRepeatedMetricRecords() {
        observabilityMetrics.recordWalletCreditSuccess();
        observabilityMetrics.recordWalletCreditSuccess();
        observabilityMetrics.recordWalletCreditSuccess();

        assertEquals(
                3.0,
                meterRegistry.get("wallet.credit.success")
                        .counter()
                        .count());
    }
}

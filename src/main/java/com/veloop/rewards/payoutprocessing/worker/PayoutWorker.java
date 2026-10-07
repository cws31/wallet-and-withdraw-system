package com.veloop.rewards.payoutprocessing.worker;

import com.veloop.rewards.audit.service.AuditLogService;
import com.veloop.rewards.audit.service.WithdrawalAuditService;
import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.provider.PayoutProvider;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderRegistry;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderRequest;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderResult;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderStatus;
import com.veloop.rewards.payoutprocessing.repository.PayoutJobRepository;
import com.veloop.rewards.payoutprocessing.retry.PayoutRetryPolicy;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PayoutWorker {

        private final PayoutJobRepository payoutJobRepository;
        private final WithdrawalRepository withdrawalRepository;
        private final PayoutProviderRegistry payoutProviderRegistry;
        private final PayoutRetryPolicy payoutRetryPolicy;
        private final WithdrawalService withdrawalService;
        private final WithdrawalAuditService withdrawalAuditService;
        private final AuditLogService auditLogService;

        public PayoutWorker(
                        PayoutJobRepository payoutJobRepository,
                        WithdrawalRepository withdrawalRepository,
                        PayoutProviderRegistry payoutProviderRegistry,
                        PayoutRetryPolicy payoutRetryPolicy,
                        WithdrawalService withdrawalService,
                        WithdrawalAuditService withdrawalAuditService,
                        AuditLogService auditLogService) {

                this.payoutJobRepository = payoutJobRepository;
                this.withdrawalRepository = withdrawalRepository;
                this.payoutProviderRegistry = payoutProviderRegistry;
                this.payoutRetryPolicy = payoutRetryPolicy;
                this.withdrawalService = withdrawalService;
                this.withdrawalAuditService = withdrawalAuditService;
                this.auditLogService = auditLogService;
        }

        @Transactional
        public PayoutProviderResult processJob(Long payoutJobId) {

                PayoutJob job = payoutJobRepository
                                .findById(payoutJobId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Payout job not found: " + payoutJobId));

                validateJobForProcessing(job);

                Withdrawal withdrawal = job.getWithdrawal();

                if (withdrawal == null) {
                        throw new IllegalStateException(
                                        "Payout job has no withdrawal");
                }

                validateWithdrawalForProcessing(withdrawal);

                if (withdrawal.getStatus() == WithdrawalStatus.PENDING) {

                        WithdrawalStatus oldStatus = withdrawal.getStatus();

                        withdrawal.setStatus(WithdrawalStatus.PROCESSING);
                        withdrawal.setUpdatedAt(LocalDateTime.now());

                        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

                        withdrawalAuditService.record(
                                        savedWithdrawal,
                                        "PROCESSING",
                                        oldStatus,
                                        WithdrawalStatus.PROCESSING,
                                        null,
                                        "Withdrawal moved to processing by payout worker");

                        auditLogService.record(
                                        null,
                                        savedWithdrawal.getUser(),
                                        "WITHDRAWAL",
                                        "WITHDRAWAL_PROCESSING",
                                        savedWithdrawal.getWithdrawalId(),
                                        "oldStatus=" + oldStatus
                                                        + ", newStatus=" + WithdrawalStatus.PROCESSING
                                                        + ", source=PAYOUT_PROCESSING");
                }

                job.setStatus(PayoutJobStatus.PROCESSING);
                job.setAttemptCount(job.getAttemptCount() + 1);
                job.setNextAttemptAt(null);
                job.setLastError(null);

                payoutJobRepository.save(job);

                String methodCode = withdrawal
                                .getPayoutMethod()
                                .getCode();

                PayoutProvider provider = payoutProviderRegistry.getProvider(methodCode);

                PayoutProviderRequest request = new PayoutProviderRequest(
                                withdrawal.getWithdrawalId(),
                                methodCode,
                                withdrawal.getCurrency(),
                                withdrawal.getCurrencyAmount(),
                                withdrawal.getPayoutAmount(),
                                withdrawal.getPayoutDetails());

                PayoutProviderResult result;

                try {

                        result = provider.process(request);

                } catch (RuntimeException exception) {

                        return handleProviderException(
                                        job,
                                        withdrawal,
                                        exception);
                }

                if (result.status() == PayoutProviderStatus.APPROVED) {

                        WithdrawalStatus oldStatus = withdrawal.getStatus();

                        LocalDateTime now = LocalDateTime.now();

                        withdrawal.setStatus(WithdrawalStatus.APPROVED);
                        withdrawal.setProcessedAt(now);
                        withdrawal.setUpdatedAt(now);

                        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

                        withdrawalAuditService.record(
                                        savedWithdrawal,
                                        "APPROVED",
                                        oldStatus,
                                        WithdrawalStatus.APPROVED,
                                        null,
                                        "Withdrawal approved by payout worker");

                        auditLogService.record(
                                        null,
                                        savedWithdrawal.getUser(),
                                        "WITHDRAWAL",
                                        "WITHDRAWAL_APPROVED",
                                        savedWithdrawal.getWithdrawalId(),
                                        "oldStatus=" + oldStatus
                                                        + ", newStatus=" + WithdrawalStatus.APPROVED
                                                        + ", source=PAYOUT_PROCESSING"
                                                        + ", providerReference="
                                                        + result.providerReference());

                        job.setStatus(PayoutJobStatus.COMPLETED);
                        job.setLastError(null);
                        job.setNextAttemptAt(null);

                        payoutJobRepository.save(job);

                        return result;
                }

                withdrawalService.rejectWithdrawalFromPayoutFailure(
                                withdrawal.getWithdrawalId(),
                                result.failureReason());

                job.setStatus(PayoutJobStatus.FAILED);
                job.setLastError(result.failureReason());
                job.setNextAttemptAt(null);

                payoutJobRepository.save(job);

                return result;
        }

        private void validateJobForProcessing(PayoutJob job) {

                PayoutJobStatus status = job.getStatus();

                if (status == PayoutJobStatus.COMPLETED) {

                        throw new IllegalStateException(
                                        "Payout job has already completed");
                }

                if (status == PayoutJobStatus.FAILED) {

                        throw new IllegalStateException(
                                        "Payout job has permanently failed");
                }

                if (status != PayoutJobStatus.QUEUED
                                && status != PayoutJobStatus.RETRY) {

                        throw new IllegalStateException(
                                        "Payout job is not eligible for processing");
                }

                if (status == PayoutJobStatus.RETRY) {

                        LocalDateTime nextAttemptAt = job.getNextAttemptAt();

                        if (nextAttemptAt != null
                                        && nextAttemptAt.isAfter(LocalDateTime.now())) {

                                throw new IllegalStateException(
                                                "Payout retry is not yet eligible");
                        }
                }
        }

        private void validateWithdrawalForProcessing(
                        Withdrawal withdrawal) {

                WithdrawalStatus status = withdrawal.getStatus();

                if (status != WithdrawalStatus.PENDING
                                && status != WithdrawalStatus.PROCESSING) {

                        throw new IllegalStateException(
                                        "Withdrawal is not eligible for payout processing: "
                                                        + status);
                }
        }

        private PayoutProviderResult handleProviderException(
                        PayoutJob job,
                        Withdrawal withdrawal,
                        RuntimeException exception) {

                String errorMessage = exception.getMessage();

                if (errorMessage == null
                                || errorMessage.isBlank()) {

                        errorMessage = exception
                                        .getClass()
                                        .getSimpleName();
                }

                if (payoutRetryPolicy.canRetry(
                                job.getAttemptCount())) {

                        LocalDateTime nextAttemptAt = LocalDateTime.now()
                                        .plus(
                                                        payoutRetryPolicy.getDelay(
                                                                        job.getAttemptCount()));

                        job.setStatus(PayoutJobStatus.RETRY);
                        job.setNextAttemptAt(nextAttemptAt);
                        job.setLastError(errorMessage);

                        payoutJobRepository.save(job);

                        auditLogService.record(
                                        null,
                                        withdrawal.getUser(),
                                        "WITHDRAWAL",
                                        "PAYOUT_PROCESSING_RETRY",
                                        withdrawal.getWithdrawalId(),
                                        "attempt="
                                                        + job.getAttemptCount()
                                                        + ", nextAttemptAt="
                                                        + nextAttemptAt
                                                        + ", error="
                                                        + errorMessage
                                                        + ", source=PAYOUT_PROCESSING");

                        return PayoutProviderResult.rejected(
                                        "Payout provider temporarily unavailable. "
                                                        + "Retry scheduled.");
                }

                withdrawalService.rejectWithdrawalFromPayoutFailure(
                                withdrawal.getWithdrawalId(),
                                "Payout provider failed after maximum "
                                                + "retry attempts: "
                                                + errorMessage);

                job.setStatus(PayoutJobStatus.FAILED);
                job.setNextAttemptAt(null);
                job.setLastError(errorMessage);

                payoutJobRepository.save(job);

                auditLogService.record(
                                null,
                                withdrawal.getUser(),
                                "WITHDRAWAL",
                                "PAYOUT_PROCESSING_FAILED",
                                withdrawal.getWithdrawalId(),
                                "attempt="
                                                + job.getAttemptCount()
                                                + ", error="
                                                + errorMessage
                                                + ", source=PAYOUT_PROCESSING");

                return PayoutProviderResult.rejected(
                                "Payout provider failed after maximum "
                                                + "retry attempts.");
        }
}
package com.veloop.rewards.payoutprocessing.worker;

import com.veloop.rewards.audit.service.AuditLogService;
import com.veloop.rewards.audit.service.WithdrawalAuditService;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.provider.FakePayoutProvider;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PayoutWorkerTest {

        private PayoutJobRepository payoutJobRepository;
        private WithdrawalRepository withdrawalRepository;
        private PayoutProviderRegistry payoutProviderRegistry;
        private PayoutRetryPolicy payoutRetryPolicy;
        private WithdrawalService withdrawalService;
        private WithdrawalAuditService withdrawalAuditService;
        private AuditLogService auditLogService;
        private PayoutWorker payoutWorker;

        @BeforeEach
        void setUp() {

                payoutJobRepository = mock(PayoutJobRepository.class);

                withdrawalRepository = mock(WithdrawalRepository.class);

                payoutProviderRegistry = mock(PayoutProviderRegistry.class);

                payoutRetryPolicy = new PayoutRetryPolicy();

                withdrawalService = mock(WithdrawalService.class);

                withdrawalAuditService = mock(WithdrawalAuditService.class);

                auditLogService = mock(AuditLogService.class);

                when(withdrawalRepository.save(any(Withdrawal.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                payoutWorker = new PayoutWorker(
                                payoutJobRepository,
                                withdrawalRepository,
                                payoutProviderRegistry,
                                payoutRetryPolicy,
                                withdrawalService,
                                withdrawalAuditService,
                                auditLogService);
        }

        @Test
        void shouldProcessApprovedPayout() {

                PayoutJob job = createJob(
                                PayoutJobStatus.QUEUED,
                                0,
                                null);

                PayoutProvider provider = new FakePayoutProvider(
                                "UPI",
                                PayoutProviderResult.approved(
                                                "PROVIDER-123"));

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                when(payoutProviderRegistry.getProvider("UPI"))
                                .thenReturn(provider);

                PayoutProviderResult result = payoutWorker.processJob(job.getId());

                assertEquals(
                                PayoutProviderStatus.APPROVED,
                                result.status());

                assertEquals(
                                WithdrawalStatus.APPROVED,
                                job.getWithdrawal().getStatus());

                assertEquals(
                                PayoutJobStatus.COMPLETED,
                                job.getStatus());

                assertEquals(
                                1,
                                job.getAttemptCount());

                assertNull(
                                job.getNextAttemptAt());

                assertNull(
                                job.getLastError());

                assertNotNull(
                                job.getWithdrawal().getProcessedAt());

                verify(
                                payoutJobRepository,
                                times(2))
                                .save(job);

                verify(
                                withdrawalRepository,
                                times(2))
                                .save(job.getWithdrawal());

                verify(withdrawalAuditService)
                                .record(
                                                job.getWithdrawal(),
                                                "PROCESSING",
                                                WithdrawalStatus.PENDING,
                                                WithdrawalStatus.PROCESSING,
                                                null,
                                                "Withdrawal moved to processing by payout worker");

                verify(withdrawalAuditService)
                                .record(
                                                job.getWithdrawal(),
                                                "APPROVED",
                                                WithdrawalStatus.PROCESSING,
                                                WithdrawalStatus.APPROVED,
                                                null,
                                                "Withdrawal approved by payout worker");

                verify(auditLogService)
                                .record(
                                                any(),
                                                any(),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL_PROCESSING"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                job.getWithdrawal()
                                                                                .getWithdrawalId()),
                                                any());

                verify(auditLogService)
                                .record(
                                                any(),
                                                any(),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL_APPROVED"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                job.getWithdrawal()
                                                                                .getWithdrawalId()),
                                                any());
        }

        @Test
        void shouldPassWithdrawalDataToProvider() {

                PayoutJob job = createJob(
                                PayoutJobStatus.QUEUED,
                                0,
                                null);

                PayoutProvider provider = mock(PayoutProvider.class);

                when(provider.process(
                                any(PayoutProviderRequest.class)))
                                .thenReturn(
                                                PayoutProviderResult.approved(
                                                                "PROVIDER-456"));

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                when(payoutProviderRegistry.getProvider("UPI"))
                                .thenReturn(provider);

                payoutWorker.processJob(job.getId());

                verify(provider)
                                .process(any(PayoutProviderRequest.class));
        }

        @Test
        void shouldRejectAlreadyProcessedJob() {

                PayoutJob job = createJob(
                                PayoutJobStatus.COMPLETED,
                                1,
                                null);

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                assertThrows(
                                IllegalStateException.class,
                                () -> payoutWorker.processJob(
                                                job.getId()));

                verifyNoInteractions(
                                payoutProviderRegistry);

                verifyNoInteractions(
                                withdrawalRepository);

                verifyNoInteractions(
                                withdrawalService);

                verifyNoInteractions(
                                withdrawalAuditService);

                verifyNoInteractions(
                                auditLogService);
        }

        @Test
        void shouldRejectNonProcessableWithdrawal() {

                PayoutJob job = createJob(
                                PayoutJobStatus.QUEUED,
                                0,
                                null);

                job.getWithdrawal()
                                .setStatus(
                                                WithdrawalStatus.APPROVED);

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                assertThrows(
                                IllegalStateException.class,
                                () -> payoutWorker.processJob(
                                                job.getId()));

                verifyNoInteractions(
                                payoutProviderRegistry);

                verifyNoInteractions(
                                withdrawalRepository);

                verifyNoInteractions(
                                withdrawalService);

                verifyNoInteractions(
                                withdrawalAuditService);

                verifyNoInteractions(
                                auditLogService);
        }

        @Test
        void shouldMarkJobFailedWhenProviderRejects() {

                PayoutJob job = createJob(
                                PayoutJobStatus.QUEUED,
                                0,
                                null);

                PayoutProvider provider = new FakePayoutProvider(
                                "UPI",
                                PayoutProviderResult.rejected(
                                                "Invalid payout details"));

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                when(payoutProviderRegistry.getProvider("UPI"))
                                .thenReturn(provider);

                PayoutProviderResult result = payoutWorker.processJob(job.getId());

                assertEquals(
                                PayoutProviderStatus.REJECTED,
                                result.status());

                assertEquals(
                                PayoutJobStatus.FAILED,
                                job.getStatus());

                assertEquals(
                                1,
                                job.getAttemptCount());

                assertEquals(
                                "Invalid payout details",
                                job.getLastError());

                assertNull(
                                job.getNextAttemptAt());

                verify(withdrawalService)
                                .rejectWithdrawalFromPayoutFailure(
                                                job.getWithdrawal()
                                                                .getWithdrawalId(),
                                                "Invalid payout details");

                assertEquals(
                                WithdrawalStatus.PROCESSING,
                                job.getWithdrawal().getStatus());

                verify(
                                payoutJobRepository,
                                times(2))
                                .save(job);

                verify(withdrawalRepository)
                                .save(job.getWithdrawal());

                verify(withdrawalAuditService)
                                .record(
                                                job.getWithdrawal(),
                                                "PROCESSING",
                                                WithdrawalStatus.PENDING,
                                                WithdrawalStatus.PROCESSING,
                                                null,
                                                "Withdrawal moved to processing by payout worker");

                verify(auditLogService)
                                .record(
                                                any(),
                                                any(),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL_PROCESSING"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                job.getWithdrawal()
                                                                                .getWithdrawalId()),
                                                any());
        }

        @Test
        void shouldScheduleRetryWhenProviderThrowsException() {

                PayoutJob job = createJob(
                                PayoutJobStatus.QUEUED,
                                0,
                                null);

                PayoutProvider provider = new PayoutProvider() {

                        @Override
                        public String getMethodCode() {
                                return "UPI";
                        }

                        @Override
                        public PayoutProviderResult process(
                                        PayoutProviderRequest request) {

                                throw new RuntimeException(
                                                "Provider timeout");
                        }
                };

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                when(payoutProviderRegistry.getProvider("UPI"))
                                .thenReturn(provider);

                PayoutProviderResult result = payoutWorker.processJob(job.getId());

                assertEquals(
                                PayoutProviderStatus.REJECTED,
                                result.status());

                assertEquals(
                                PayoutJobStatus.RETRY,
                                job.getStatus());

                assertEquals(
                                1,
                                job.getAttemptCount());

                assertNotNull(
                                job.getNextAttemptAt());

                assertTrue(
                                job.getNextAttemptAt()
                                                .isAfter(LocalDateTime.now()));

                assertEquals(
                                "Provider timeout",
                                job.getLastError());

                assertEquals(
                                WithdrawalStatus.PROCESSING,
                                job.getWithdrawal().getStatus());

                verifyNoInteractions(
                                withdrawalService);

                verify(
                                payoutJobRepository,
                                times(2))
                                .save(job);

                verify(withdrawalRepository)
                                .save(job.getWithdrawal());

                verify(withdrawalAuditService)
                                .record(
                                                job.getWithdrawal(),
                                                "PROCESSING",
                                                WithdrawalStatus.PENDING,
                                                WithdrawalStatus.PROCESSING,
                                                null,
                                                "Withdrawal moved to processing by payout worker");

                verify(auditLogService)
                                .record(
                                                any(),
                                                any(),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "PAYOUT_PROCESSING_RETRY"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                job.getWithdrawal()
                                                                                .getWithdrawalId()),
                                                any());
        }

        @Test
        void shouldNotProcessRetryBeforeNextAttemptTime() {

                PayoutJob job = createJob(
                                PayoutJobStatus.RETRY,
                                1,
                                LocalDateTime.now()
                                                .plusMinutes(5));

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                assertThrows(
                                IllegalStateException.class,
                                () -> payoutWorker.processJob(
                                                job.getId()));

                verifyNoInteractions(
                                payoutProviderRegistry);

                verifyNoInteractions(
                                withdrawalRepository);

                verifyNoInteractions(
                                withdrawalService);

                verifyNoInteractions(
                                withdrawalAuditService);

                verifyNoInteractions(
                                auditLogService);
        }

        @Test
        void shouldProcessEligibleRetry() {

                PayoutJob job = createJob(
                                PayoutJobStatus.RETRY,
                                1,
                                LocalDateTime.now()
                                                .minusSeconds(1));

                job.getWithdrawal()
                                .setStatus(
                                                WithdrawalStatus.PROCESSING);

                PayoutProvider provider = new FakePayoutProvider(
                                "UPI",
                                PayoutProviderResult.approved(
                                                "RETRY-SUCCESS-123"));

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                when(payoutProviderRegistry.getProvider("UPI"))
                                .thenReturn(provider);

                PayoutProviderResult result = payoutWorker.processJob(job.getId());

                assertEquals(
                                PayoutProviderStatus.APPROVED,
                                result.status());

                assertEquals(
                                PayoutJobStatus.COMPLETED,
                                job.getStatus());

                assertEquals(
                                2,
                                job.getAttemptCount());

                assertNull(
                                job.getNextAttemptAt());

                assertNull(
                                job.getLastError());

                assertEquals(
                                WithdrawalStatus.APPROVED,
                                job.getWithdrawal().getStatus());

                verifyNoInteractions(
                                withdrawalService);

                verify(
                                payoutJobRepository,
                                times(2))
                                .save(job);

                verify(withdrawalRepository)
                                .save(job.getWithdrawal());

                verify(withdrawalAuditService)
                                .record(
                                                job.getWithdrawal(),
                                                "APPROVED",
                                                WithdrawalStatus.PROCESSING,
                                                WithdrawalStatus.APPROVED,
                                                null,
                                                "Withdrawal approved by payout worker");

                verify(auditLogService)
                                .record(
                                                any(),
                                                any(),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL_APPROVED"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                job.getWithdrawal()
                                                                                .getWithdrawalId()),
                                                any());
        }

        @Test
        void shouldPermanentlyFailAfterMaximumRetryAttempts() {

                PayoutJob job = createJob(
                                PayoutJobStatus.RETRY,
                                2,
                                LocalDateTime.now()
                                                .minusSeconds(1));

                job.getWithdrawal()
                                .setStatus(
                                                WithdrawalStatus.PROCESSING);

                PayoutProvider provider = new PayoutProvider() {

                        @Override
                        public String getMethodCode() {
                                return "UPI";
                        }

                        @Override
                        public PayoutProviderResult process(
                                        PayoutProviderRequest request) {

                                throw new RuntimeException(
                                                "Provider unavailable");
                        }
                };

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                when(payoutProviderRegistry.getProvider("UPI"))
                                .thenReturn(provider);

                PayoutProviderResult result = payoutWorker.processJob(job.getId());

                assertEquals(
                                PayoutProviderStatus.REJECTED,
                                result.status());

                assertEquals(
                                PayoutJobStatus.FAILED,
                                job.getStatus());

                assertEquals(
                                3,
                                job.getAttemptCount());

                assertNull(
                                job.getNextAttemptAt());

                assertEquals(
                                "Provider unavailable",
                                job.getLastError());

                verify(withdrawalService)
                                .rejectWithdrawalFromPayoutFailure(
                                                job.getWithdrawal()
                                                                .getWithdrawalId(),
                                                "Payout provider failed after maximum "
                                                                + "retry attempts: Provider unavailable");

                verify(
                                payoutJobRepository,
                                times(2))
                                .save(job);

                assertEquals(
                                WithdrawalStatus.PROCESSING,
                                job.getWithdrawal().getStatus());

                verifyNoInteractions(
                                withdrawalAuditService);

                verify(auditLogService)
                                .record(
                                                any(),
                                                any(),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "WITHDRAWAL"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                "PAYOUT_PROCESSING_FAILED"),
                                                org.mockito.ArgumentMatchers.eq(
                                                                job.getWithdrawal()
                                                                                .getWithdrawalId()),
                                                any());
        }

        @Test
        void shouldRejectAlreadyFailedJob() {

                PayoutJob job = createJob(
                                PayoutJobStatus.FAILED,
                                3,
                                null);

                when(payoutJobRepository.findById(job.getId()))
                                .thenReturn(Optional.of(job));

                assertThrows(
                                IllegalStateException.class,
                                () -> payoutWorker.processJob(
                                                job.getId()));

                verifyNoInteractions(
                                payoutProviderRegistry);

                verifyNoInteractions(
                                withdrawalRepository);

                verifyNoInteractions(
                                withdrawalService);

                verifyNoInteractions(
                                withdrawalAuditService);

                verifyNoInteractions(
                                auditLogService);
        }

        private PayoutJob createJob(
                        PayoutJobStatus status,
                        int attemptCount,
                        LocalDateTime nextAttemptAt) {

                Withdrawal withdrawal = new Withdrawal();

                withdrawal.setId(100L);

                withdrawal.setWithdrawalId(
                                "WD-TEST-100");

                withdrawal.setCurrency(
                                "VES");

                withdrawal.setCurrencyAmount(
                                new BigDecimal("2400"));

                withdrawal.setPayoutAmount(
                                new BigDecimal("10"));

                withdrawal.setPayoutDetails(
                                "9876543210@ybl");

                withdrawal.setStatus(
                                WithdrawalStatus.PENDING);

                PayoutMethod payoutMethod = new PayoutMethod();

                payoutMethod.setId(1L);

                payoutMethod.setCode("UPI");

                payoutMethod.setName("UPI");

                payoutMethod.setActive(true);

                withdrawal.setPayoutMethod(
                                payoutMethod);

                PayoutJob job = new PayoutJob();

                job.setId(1L);

                job.setWithdrawal(
                                withdrawal);

                job.setStatus(
                                status);

                job.setAttemptCount(
                                attemptCount);

                job.setNextAttemptAt(
                                nextAttemptAt);

                return job;
        }
}
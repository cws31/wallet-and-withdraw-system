package com.veloop.rewards.payoutprocessing.worker;

import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;

import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.provider.PayoutProvider;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderRegistry;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderRequest;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderResult;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderStatus;
import com.veloop.rewards.payoutprocessing.queue.DatabasePayoutQueue;
import com.veloop.rewards.payoutprocessing.repository.PayoutJobRepository;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;

import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.service.WalletService;

import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class PayoutWorkerConcurrencyIntegrationTest {

    @Autowired
    private PayoutWorker payoutWorker;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private DatabasePayoutQueue payoutQueue;

    @Autowired
    private PayoutJobRepository payoutJobRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Autowired
    private PayoutOptionRepository payoutOptionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PayoutProviderRegistry payoutProviderRegistry;

    private Long testUserId;

    private Long payoutJobId;

    private String withdrawalId;

    private PayoutMethod upiMethod;

    private PayoutOption tenRupeeOption;

    @BeforeEach
    void setUp() {

        String uniqueId = UUID.randomUUID().toString();

        User user = User.builder()
                .name("Payout Worker Concurrency User")
                .email(
                        "payout-worker-concurrency-"
                                + uniqueId
                                + "@example.com")
                .passwordHash("test-password-hash")
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(true)
                .level(0)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        testUserId = savedUser.getId();

        walletService.createWallet(
                testUserId);

        upiMethod = payoutMethodRepository
                .findByCode("UPI")
                .orElseThrow(() -> new IllegalStateException(
                        "UPI payout method not found"));

        tenRupeeOption = new PayoutOption();

        tenRupeeOption.setMethod(
                upiMethod);

        tenRupeeOption.setPayoutAmount(
                new BigDecimal("10"));

        tenRupeeOption.setCurrency(
                "INR");

        tenRupeeOption.setCurrencyAmount(
                new BigDecimal("2400"));

        tenRupeeOption.setActive(
                true);

        tenRupeeOption = payoutOptionRepository
                .saveAndFlush(
                        tenRupeeOption);

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("10000"),
                        TransactionType.REWARD,
                        "TEST",
                        "PAYOUT-WORKER-SETUP-"
                                + uniqueId,
                        "Payout worker concurrency test",
                        null));

        WithdrawalCreateRequest request = new WithdrawalCreateRequest();

        request.setPayoutMethodId(
                upiMethod.getId());

        request.setPayoutOptionId(
                tenRupeeOption.getId());

        request.setPayoutDetails(
                "9876543210@ybl");

        WithdrawalResponse withdrawalResponse = withdrawalService.createWithdrawal(
                testUserId,
                "worker-concurrency-"
                        + uniqueId,
                request);

        assertNotNull(
                withdrawalResponse);

        withdrawalId = withdrawalResponse.getWithdrawalId();

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(
                        withdrawalId)
                .orElseThrow();

        assertEquals(
                WithdrawalStatus.PENDING,
                withdrawal.getStatus());

        PayoutJob job = payoutQueue.enqueue(
                withdrawal.getId());

        assertNotNull(job);

        payoutJobId = job.getId();
    }

    @AfterEach
    void tearDown() {

        if (testUserId == null) {
            return;
        }

        jdbcTemplate.update(
                """
                        DELETE FROM payout_jobs
                        WHERE withdrawal_id IN (
                            SELECT id
                            FROM withdrawals
                            WHERE user_id = ?
                        )
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM withdrawal_idempotency
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM withdrawal_audit
                        WHERE withdrawal_id IN (
                            SELECT id
                            FROM withdrawals
                            WHERE user_id = ?
                        )
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM audit_log
                        WHERE target_user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM audit_log
                        WHERE actor_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM fraud_risk_events
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM withdrawals
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM wallet_transactions
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM wallets
                        WHERE user_id = ?
                        """,
                testUserId);

        if (tenRupeeOption != null
                && tenRupeeOption.getId() != null) {

            jdbcTemplate.update(
                    """
                            DELETE FROM payout_options
                            WHERE id = ?
                            """,
                    tenRupeeOption.getId());
        }

        jdbcTemplate.update(
                """
                        DELETE FROM users
                        WHERE id = ?
                        """,
                testUserId);
    }

    @Test
    void shouldPreventConcurrentDuplicatePayoutProcessing()
            throws Exception {

        CountDownLatch firstProviderStarted = new CountDownLatch(1);

        CountDownLatch releaseFirstProvider = new CountDownLatch(1);

        PayoutProvider provider = mock(PayoutProvider.class);

        when(
                provider.process(
                        any(PayoutProviderRequest.class)))
                .thenAnswer(invocation -> {

                    firstProviderStarted.countDown();

                    boolean released = releaseFirstProvider.await(
                            10,
                            TimeUnit.SECONDS);

                    if (!released) {

                        throw new IllegalStateException(
                                "Test timed out while waiting "
                                        + "to release provider");
                    }

                    return PayoutProviderResult.approved(
                            "CONCURRENT-PROVIDER-REF");
                });

        when(
                payoutProviderRegistry.getProvider("UPI"))
                .thenReturn(provider);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<PayoutProviderResult> firstWorker = executor.submit(() -> payoutWorker.processJob(
                    payoutJobId));

            assertTrue(
                    firstProviderStarted.await(
                            10,
                            TimeUnit.SECONDS),
                    "First worker did not reach provider");

            Future<PayoutProviderResult> secondWorker = executor.submit(() -> payoutWorker.processJob(
                    payoutJobId));

            Thread.sleep(500);

            releaseFirstProvider.countDown();

            PayoutProviderResult firstResult = firstWorker.get(
                    10,
                    TimeUnit.SECONDS);

            assertEquals(
                    PayoutProviderStatus.APPROVED,
                    firstResult.status());

            Exception secondException = null;

            try {

                secondWorker.get(
                        10,
                        TimeUnit.SECONDS);

            } catch (Exception exception) {

                secondException = exception;
            }

            assertNotNull(
                    secondException,
                    "Second worker must not process "
                            + "an already completed payout");

            verify(
                    provider,
                    times(1))
                    .process(
                            any(PayoutProviderRequest.class));

            String finalStatus = jdbcTemplate.queryForObject(
                    """
                            SELECT status
                            FROM payout_jobs
                            WHERE id = ?
                            """,
                    String.class,
                    payoutJobId);

            Integer finalAttemptCount = jdbcTemplate.queryForObject(
                    """
                            SELECT attempt_count
                            FROM payout_jobs
                            WHERE id = ?
                            """,
                    Integer.class,
                    payoutJobId);

            assertEquals(
                    PayoutJobStatus.COMPLETED.name(),
                    finalStatus);

            assertEquals(
                    1,
                    finalAttemptCount);

            Withdrawal finalWithdrawal = withdrawalRepository
                    .findByWithdrawalId(
                            withdrawalId)
                    .orElseThrow();

            assertEquals(
                    WithdrawalStatus.APPROVED,
                    finalWithdrawal.getStatus());

        } finally {

            releaseFirstProvider.countDown();

            executor.shutdownNow();

            executor.awaitTermination(
                    5,
                    TimeUnit.SECONDS);
        }
    }
}

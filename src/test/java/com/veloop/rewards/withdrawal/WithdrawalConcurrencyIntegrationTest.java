package com.veloop.rewards.withdrawal;

import com.veloop.rewards.common.exception.IdempotencyConflictException;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WithdrawalConcurrencyIntegrationTest {

        @Autowired
        private WithdrawalService withdrawalService;

        @Autowired
        private WalletService walletService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private WalletRepository walletRepository;

        @Autowired
        private WalletTransactionRepository walletTransactionRepository;

        @Autowired
        private WithdrawalRepository withdrawalRepository;

        @Autowired
        private PayoutMethodRepository payoutMethodRepository;

        @Autowired
        private PayoutOptionRepository payoutOptionRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private Long testUserId;

        private PayoutMethod upiMethod;

        private PayoutOption tenRupeeOption;

        @BeforeEach
        void setUp() {

                String uniqueId = String.valueOf(System.nanoTime());

                User user = User.builder()
                                .name("Concurrency Test User")
                                .email("concurrency-" + uniqueId + "@example.com")
                                .passwordHash("test-password-hash")
                                .role("USER")
                                .accountStatus("ACTIVE")
                                .verified(true)
                                .level(0)
                                .build();

                User savedUser = userRepository.saveAndFlush(user);
                testUserId = savedUser.getId();

                walletService.createWallet(testUserId);

                upiMethod = payoutMethodRepository
                                .findByCode("UPI")
                                .orElseThrow(() -> new IllegalStateException(
                                                "UPI payout method not found"));

                tenRupeeOption = new PayoutOption();
                tenRupeeOption.setMethod(upiMethod);
                tenRupeeOption.setPayoutAmount(new BigDecimal("10"));
                tenRupeeOption.setCurrency("INR");
                tenRupeeOption.setCurrencyAmount(new BigDecimal("2400"));
                tenRupeeOption.setActive(true);

                tenRupeeOption = payoutOptionRepository.saveAndFlush(
                                tenRupeeOption);

                walletService.creditWallet(
                                testUserId,
                                new WalletCreditRequest(
                                                Currency.VES,
                                                new BigDecimal("10000"),
                                                TransactionType.REWARD,
                                                "TEST",
                                                "CONCURRENCY-SETUP-" + uniqueId,
                                                "Concurrency test wallet credit",
                                                null));
        }

        @AfterEach
        void tearDown() {

                jdbcTemplate.update("""
                                DELETE FROM withdrawal_idempotency
                                WHERE user_id = ?
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM audit_log
                                WHERE target_user_id = ?
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM audit_log
                                WHERE actor_id = ?
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM withdrawal_audit
                                WHERE withdrawal_id IN (
                                    SELECT id
                                    FROM withdrawals
                                    WHERE user_id = ?
                                )
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM fraud_risk_events
                                WHERE user_id = ?
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM withdrawals
                                WHERE user_id = ?
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM wallet_transactions
                                WHERE user_id = ?
                                """,
                                testUserId);

                jdbcTemplate.update("""
                                DELETE FROM wallets
                                WHERE user_id = ?
                                """,
                                testUserId);

                if (tenRupeeOption != null
                                && tenRupeeOption.getId() != null) {

                        jdbcTemplate.update("""
                                        DELETE FROM payout_options
                                        WHERE id = ?
                                        """,
                                        tenRupeeOption.getId());
                }

                jdbcTemplate.update("""
                                DELETE FROM users
                                WHERE id = ?
                                """,
                                testUserId);
        }

        @Test
        void shouldPreventDuplicateWithdrawalForConcurrentSameKeyRequests()
                        throws Exception {

                String idempotencyKey = "concurrent-" + System.nanoTime();

                WithdrawalCreateRequest request = createRequest(
                                upiMethod.getId(),
                                tenRupeeOption.getId(),
                                "concurrent@upi");

                ExecutorService executor = Executors.newFixedThreadPool(2);

                CountDownLatch startLatch = new CountDownLatch(1);

                Future<WithdrawalResponse> firstRequest = executor.submit(() -> {

                        startLatch.await();

                        return withdrawalService.createWithdrawal(
                                        testUserId,
                                        idempotencyKey,
                                        request);
                });

                Future<WithdrawalResponse> secondRequest = executor.submit(() -> {

                        startLatch.await();

                        return withdrawalService.createWithdrawal(
                                        testUserId,
                                        idempotencyKey,
                                        request);
                });

                startLatch.countDown();

                WithdrawalResponse firstResponse = null;
                WithdrawalResponse secondResponse = null;

                Exception firstException = null;
                Exception secondException = null;

                try {
                        firstResponse = firstRequest.get();
                } catch (Exception ex) {
                        firstException = ex;
                }

                try {
                        secondResponse = secondRequest.get();
                } catch (Exception ex) {
                        secondException = ex;
                }

                executor.shutdown();

                int successfulRequests = 0;

                if (firstResponse != null) {
                        successfulRequests++;
                }

                if (secondResponse != null) {
                        successfulRequests++;
                }

                assertEquals(
                                1,
                                successfulRequests,
                                "Exactly one concurrent request should create the withdrawal");

                assertNotNull(
                                firstResponse != null
                                                ? firstResponse
                                                : secondResponse);

                Exception failedException = firstResponse == null
                                ? firstException
                                : secondException;

                assertNotNull(failedException);

                Wallet wallet = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                wallet.getVes()
                                                .compareTo(
                                                                new BigDecimal("7600")));

                List<Withdrawal> withdrawals = withdrawalRepository
                                .findByUserIdOrderByCreatedAtDesc(
                                                testUserId,
                                                PageRequest.of(
                                                                0,
                                                                20))
                                .getContent();

                assertEquals(
                                1,
                                withdrawals.size());

                assertEquals(
                                WithdrawalStatus.PENDING,
                                withdrawals.get(0).getStatus());

                List<WalletTransaction> transactions = walletTransactionRepository
                                .findByUserIdOrderByCreatedAtDesc(
                                                testUserId,
                                                PageRequest.of(
                                                                0,
                                                                20))
                                .getContent();

                long withdrawalTransactions = transactions.stream()
                                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                                .count();

                assertEquals(
                                1,
                                withdrawalTransactions);

                assertTrue(
                                failedException.getCause() instanceof IdempotencyConflictException
                                                || failedException.getCause() instanceof RuntimeException
                                                || failedException instanceof RuntimeException,
                                "The losing concurrent request should fail");

                assertEquals(
                                withdrawals.get(0)
                                                .getWithdrawalId(),
                                firstResponse != null
                                                ? firstResponse.getWithdrawalId()
                                                : secondResponse.getWithdrawalId());
        }

        private WithdrawalCreateRequest createRequest(
                        Long payoutMethodId,
                        Long payoutOptionId,
                        String payoutDetails) {

                WithdrawalCreateRequest request = new WithdrawalCreateRequest();

                request.setPayoutMethodId(
                                payoutMethodId);

                request.setPayoutOptionId(
                                payoutOptionId);

                request.setPayoutDetails(
                                payoutDetails);

                return request;
        }
}

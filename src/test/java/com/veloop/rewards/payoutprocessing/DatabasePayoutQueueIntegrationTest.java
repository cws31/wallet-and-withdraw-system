package com.veloop.rewards.payoutprocessing;

import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.queue.PayoutQueue;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DatabasePayoutQueueIntegrationTest {

        @Autowired
        private PayoutQueue payoutQueue;

        @Autowired
        private WithdrawalService withdrawalService;

        @Autowired
        private WalletService walletService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PayoutMethodRepository payoutMethodRepository;

        @Autowired
        private PayoutOptionRepository payoutOptionRepository;

        @Autowired
        private WithdrawalRepository withdrawalRepository;

        private Long testUserId;

        private PayoutMethod upiMethod;

        private PayoutOption payoutOption;

        @BeforeEach
        void setUp() {

                String uniqueId = String.valueOf(System.nanoTime());

                User user = new User();

                user.setEmail(
                                "payout-queue-user-"
                                                + uniqueId
                                                + "@test.com");

                user.setPasswordHash("test-password");

                user.setName("Payout Queue Test User");

                user.setAccountStatus("ACTIVE");

                user.setVerified(true);

                user = userRepository.save(user);

                testUserId = user.getId();

                walletService.createWallet(testUserId);

                upiMethod = payoutMethodRepository
                                .findByCode("UPI")
                                .orElseGet(() -> {

                                        PayoutMethod method = new PayoutMethod();

                                        method.setCode("UPI");

                                        method.setName("UPI");

                                        method.setActive(true);

                                        return payoutMethodRepository
                                                        .saveAndFlush(method);
                                });

                payoutOption = payoutOptionRepository
                                .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                                                upiMethod.getId())
                                .stream()
                                .findFirst()
                                .orElseGet(() -> {

                                        PayoutOption option = new PayoutOption();

                                        option.setMethod(upiMethod);

                                        option.setPayoutAmount(
                                                        new BigDecimal("10"));

                                        option.setCurrency("INR");

                                        option.setCurrencyAmount(
                                                        new BigDecimal("2400"));

                                        option.setActive(true);

                                        return payoutOptionRepository
                                                        .saveAndFlush(option);
                                });
        }

        @Test
        void shouldEnqueueExistingPendingWithdrawal() {

                creditWallet(
                                new BigDecimal("5000"),
                                "PAYOUT-QUEUE-CREDIT-001");

                WithdrawalResponse withdrawal = withdrawalService.createWithdrawal(
                                testUserId,
                                "PAYOUT-QUEUE-IDEMPOTENCY-001",
                                createWithdrawalRequest(
                                                "9876543210@ybl"));

                assertNotNull(withdrawal);

                assertNotNull(
                                withdrawal.getWithdrawalId());

                assertEquals(
                                "PENDING",
                                withdrawal.getStatus());

                Long withdrawalRecordId = withdrawalRepository
                                .findByWithdrawalId(
                                                withdrawal.getWithdrawalId())
                                .orElseThrow()
                                .getId();

                PayoutJob job = payoutQueue.enqueue(
                                withdrawalRecordId);

                assertNotNull(job);

                assertNotNull(job.getId());

                assertEquals(
                                PayoutJobStatus.QUEUED,
                                job.getStatus());

                assertEquals(
                                0,
                                job.getAttemptCount());

                assertEquals(
                                withdrawalRecordId,
                                job.getWithdrawal().getId());

                assertNotNull(
                                job.getNextAttemptAt());
        }

        @Test
        void shouldReturnSameJobWhenWithdrawalIsEnqueuedTwice() {

                creditWallet(
                                new BigDecimal("5000"),
                                "PAYOUT-QUEUE-CREDIT-002");

                WithdrawalResponse withdrawal = withdrawalService.createWithdrawal(
                                testUserId,
                                "PAYOUT-QUEUE-IDEMPOTENCY-002",
                                createWithdrawalRequest(
                                                "9876543211@ybl"));

                assertNotNull(withdrawal);

                assertEquals(
                                "PENDING",
                                withdrawal.getStatus());

                Long withdrawalRecordId = withdrawalRepository
                                .findByWithdrawalId(
                                                withdrawal.getWithdrawalId())
                                .orElseThrow()
                                .getId();

                PayoutJob firstJob = payoutQueue.enqueue(
                                withdrawalRecordId);

                PayoutJob secondJob = payoutQueue.enqueue(
                                withdrawalRecordId);

                assertNotNull(firstJob);

                assertNotNull(secondJob);

                assertEquals(
                                firstJob.getId(),
                                secondJob.getId());

                assertEquals(
                                PayoutJobStatus.QUEUED,
                                secondJob.getStatus());

                assertEquals(
                                0,
                                secondJob.getAttemptCount());
        }

        private WithdrawalCreateRequest createWithdrawalRequest(
                        String payoutDetails) {

                WithdrawalCreateRequest request = new WithdrawalCreateRequest();

                request.setPayoutMethodId(
                                upiMethod.getId());

                request.setPayoutOptionId(
                                payoutOption.getId());

                request.setPayoutDetails(
                                payoutDetails);

                return request;
        }

        private void creditWallet(
                        BigDecimal amount,
                        String referenceId) {

                WalletCreditRequest request = new WalletCreditRequest(
                                Currency.VES,
                                amount,
                                TransactionType.REWARD,
                                "PAYOUT_QUEUE_TEST",
                                referenceId,
                                "Payout queue integration test",
                                null);

                walletService.creditWallet(
                                testUserId,
                                request);
        }
}
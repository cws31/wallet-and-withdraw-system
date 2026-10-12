package com.veloop.rewards.payoutprocessing;

import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.queue.PayoutQueue;
import com.veloop.rewards.payoutprocessing.repository.PayoutJobRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PayoutQueueIntegrationTest {

        @Autowired
        private PayoutQueue payoutQueue;

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

        private Withdrawal withdrawal;

        private PayoutMethod payoutMethod;

        private PayoutOption payoutOption;

        @BeforeEach
        void setUp() {

                User user = User.builder()
                                .name("Payout Queue Test User")
                                .email(
                                                "payout-queue-"
                                                                + System.nanoTime()
                                                                + "@example.com")
                                .passwordHash("test-password")
                                .role("USER")
                                .accountStatus("ACTIVE")
                                .verified(false)
                                .level(0)
                                .build();

                user = userRepository.saveAndFlush(user);

                Wallet wallet = walletService.createWallet(user.getId());

                assertNotNull(wallet);

                payoutMethod = new PayoutMethod();

                payoutMethod.setCode(
                                "TEST_QUEUE_" + System.nanoTime());

                payoutMethod.setName("Queue Test Payout Method");
                payoutMethod.setActive(true);

                payoutMethod = payoutMethodRepository.saveAndFlush(
                                payoutMethod);

                payoutOption = new PayoutOption();

                payoutOption.setMethod(payoutMethod);
                payoutOption.setPayoutAmount(
                                BigDecimal.valueOf(1000));
                payoutOption.setCurrency("INR");
                payoutOption.setCurrencyAmount(
                                BigDecimal.valueOf(1000));
                payoutOption.setActive(true);

                payoutOption = payoutOptionRepository.saveAndFlush(
                                payoutOption);

                withdrawal = new Withdrawal();

                withdrawal.setWithdrawalId(
                                "WD-QUEUE-" + System.nanoTime());

                withdrawal.setUser(user);

                withdrawal.setPayoutMethod(payoutMethod);

                withdrawal.setPayoutOption(payoutOption);

                withdrawal.setCurrency("VES");

                withdrawal.setCurrencyAmount(
                                BigDecimal.valueOf(1000));

                withdrawal.setPayoutAmount(
                                BigDecimal.valueOf(1000));

                withdrawal.setPayoutDetails(
                                "queue-test");

                withdrawal.setStatus(
                                WithdrawalStatus.PENDING);

                withdrawal.setRequestedAt(
                                LocalDateTime.now());

                withdrawal.setCreatedAt(
                                LocalDateTime.now());

                withdrawal.setUpdatedAt(
                                LocalDateTime.now());

                withdrawal = withdrawalRepository.saveAndFlush(
                                withdrawal);

                assertNotNull(withdrawal.getId());
        }

        @Test
        void shouldEnqueueWithdrawal() {

                PayoutJob job = payoutQueue.enqueue(
                                withdrawal.getId());

                assertNotNull(job);

                assertNotNull(job.getId());

                assertEquals(
                                withdrawal.getId(),
                                job.getWithdrawal().getId());

                assertEquals(
                                PayoutJobStatus.QUEUED,
                                job.getStatus());

                assertEquals(
                                0,
                                job.getAttemptCount());

                PayoutJob persistedJob = payoutJobRepository
                                .findById(job.getId())
                                .orElseThrow();

                assertEquals(
                                withdrawal.getId(),
                                persistedJob.getWithdrawal().getId());

                assertEquals(
                                PayoutJobStatus.QUEUED,
                                persistedJob.getStatus());

                assertEquals(
                                0,
                                persistedJob.getAttemptCount());
        }

        @Test
        void shouldReturnExistingJobForSameWithdrawal() {

                PayoutJob firstJob = payoutQueue.enqueue(
                                withdrawal.getId());

                PayoutJob secondJob = payoutQueue.enqueue(
                                withdrawal.getId());

                assertNotNull(firstJob);
                assertNotNull(secondJob);

                assertEquals(
                                firstJob.getId(),
                                secondJob.getId());

                assertEquals(
                                withdrawal.getId(),
                                secondJob.getWithdrawal().getId());

                assertEquals(
                                PayoutJobStatus.QUEUED,
                                secondJob.getStatus());

                assertEquals(
                                1,
                                payoutJobRepository
                                                .findByWithdrawalId(withdrawal.getId())
                                                .stream()
                                                .count());
        }
}

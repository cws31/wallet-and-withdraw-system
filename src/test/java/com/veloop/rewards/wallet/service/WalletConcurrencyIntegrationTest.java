package com.veloop.rewards.wallet.service;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletConcurrencyIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Concurrency Test User")
                .email(
                        "concurrency-"
                                + System.nanoTime()
                                + "@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        walletService.createWallet(
                savedUser.getId());

        testUserId = savedUser.getId();

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("10000"),
                        TransactionType.REWARD,
                        "TEST",
                        "CONCURRENCY-INITIAL",
                        "Initial concurrency test balance",
                        null));
    }

    @Test
    void shouldAllowOnlyOneConcurrentDebit() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Boolean> debitTask = () -> {

            startLatch.await();

            try {

                walletService.debitWallet(
                        testUserId,
                        new WalletDebitRequest(
                                Currency.VES,
                                new BigDecimal("8000"),
                                TransactionType.WITHDRAWAL,
                                "TEST",
                                "CONCURRENT-"
                                        + System.nanoTime(),
                                "Concurrent debit test",
                                null));

                return true;

            } catch (ObjectOptimisticLockingFailureException exception) {

                return false;

            } catch (RuntimeException exception) {

                return false;
            }
        };

        Future<Boolean> requestA = executor.submit(debitTask);

        Future<Boolean> requestB = executor.submit(debitTask);

        startLatch.countDown();

        boolean resultA = requestA.get(
                10,
                TimeUnit.SECONDS);

        boolean resultB = requestB.get(
                10,
                TimeUnit.SECONDS);

        executor.shutdown();

        int successfulRequests = 0;

        if (resultA) {
            successfulRequests++;
        }

        if (resultB) {
            successfulRequests++;
        }

        assertEquals(
                1,
                successfulRequests);

        Wallet finalWallet = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                finalWallet
                        .getVes()
                        .compareTo(
                                new BigDecimal("2000")));
    }
}
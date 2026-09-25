package com.veloop.rewards.wallet.service;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class WalletAtomicityIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private WalletTransactionService walletTransactionService;

    private Long testUserId;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Atomicity Test User")
                .email(
                        "atomicity-"
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
    }

    @Test
    void shouldRollbackWalletUpdateWhenLedgerCreationFails() {

        String referenceId = "ATOMICITY-" + System.nanoTime();

        doThrow(
                new RuntimeException(
                        "Simulated ledger failure"))
                .when(walletTransactionService)
                .createTransaction(
                        anyLong(),
                        anyLong(),
                        any(Currency.class),
                        any(TransactionType.class),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        anyString(),
                        anyString(),
                        any(TransactionStatus.class),
                        anyString(),
                        any());

        assertThrows(
                RuntimeException.class,
                () -> walletService.creditWallet(
                        testUserId,
                        new WalletCreditRequest(
                                Currency.VES,
                                new BigDecimal("1000"),
                                TransactionType.REWARD,
                                "TEST",
                                referenceId,
                                "Atomicity test",
                                null)));

        Wallet walletAfterFailure = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                walletAfterFailure
                        .getVes()
                        .compareTo(BigDecimal.ZERO));

        assertTrue(
                walletTransactionRepository
                        .findByReferenceId(referenceId)
                        .isEmpty());
    }
}
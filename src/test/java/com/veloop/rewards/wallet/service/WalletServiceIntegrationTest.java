
package com.veloop.rewards.wallet.service;

import com.veloop.rewards.common.exception.InsufficientBalanceException;
import com.veloop.rewards.common.exception.InvalidAmountException;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WalletServiceIntegrationTest {

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

    private Long testUserId;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Wallet Test User")
                .email("wallet-test@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.save(user);

        walletService.createWallet(savedUser.getId());

        testUserId = savedUser.getId();
    }

    @Test
    void shouldCreditVesSuccessfully() {

        Wallet wallet = walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-001",
                        "Test reward credit",
                        null));

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("1000")));
    }

    @Test
    void shouldCreditAndDebitVesSuccessfully() {

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("1500"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-002",
                        "Test reward credit",
                        null));

        Wallet wallet = walletService.debitWallet(
                testUserId,
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.WITHDRAWAL,
                        "TEST",
                        "TEST-DEBIT-001",
                        "Test withdrawal debit",
                        null));

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("500")));
    }

    @Test
    void shouldRejectDebitWhenBalanceIsInsufficient() {

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("500"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-003",
                        "Test reward credit",
                        null));

        assertThrows(
                InsufficientBalanceException.class,
                () -> walletService.debitWallet(
                        testUserId,
                        new WalletDebitRequest(
                                Currency.VES,
                                new BigDecimal("1000"),
                                TransactionType.WITHDRAWAL,
                                "TEST",
                                "TEST-DEBIT-002",
                                "Insufficient balance test",
                                null)));
    }

    @Test
    void shouldRejectZeroAmount() {

        assertThrows(
                InvalidAmountException.class,
                () -> walletService.creditWallet(
                        testUserId,
                        new WalletCreditRequest(
                                Currency.VES,
                                BigDecimal.ZERO,
                                TransactionType.REWARD,
                                "TEST",
                                "TEST-004",
                                "Invalid zero credit",
                                null)));
    }

    @Test
    void shouldRejectNegativeAmount() {

        assertThrows(
                InvalidAmountException.class,
                () -> walletService.debitWallet(
                        testUserId,
                        new WalletDebitRequest(
                                Currency.VES,
                                new BigDecimal("-100"),
                                TransactionType.WITHDRAWAL,
                                "TEST",
                                "TEST-DEBIT-003",
                                "Negative amount test",
                                null)));
    }

    @Test
    void shouldKeepCurrenciesIndependent() {

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-005",
                        "VES test credit",
                        null));

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.GEMS,
                        new BigDecimal("250"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-006",
                        "Gems test credit",
                        null));

        Wallet wallet = walletService.getWallet(testUserId);

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("1000")));

        assertEquals(
                0,
                wallet.getGems().compareTo(
                        new BigDecimal("250")));

        assertEquals(
                0,
                wallet.getSves().compareTo(
                        BigDecimal.ZERO));

        assertEquals(
                0,
                wallet.getTokens().compareTo(
                        BigDecimal.ZERO));

        assertEquals(
                0,
                wallet.getSpins().compareTo(
                        BigDecimal.ZERO));
    }

    @Test
    void shouldCreateLedgerEntryWhenWalletIsCredited() {

        Wallet wallet = walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-LEDGER-001",
                        "Test reward credit",
                        null));

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("1000")));

        WalletTransaction transaction = walletTransactionRepository
                .findByReferenceId("TEST-LEDGER-001")
                .orElseThrow();

        assertNotNull(transaction.getTransactionId());

        assertTrue(
                transaction.getTransactionId()
                        .startsWith("TXN-"));

        assertEquals(
                testUserId,
                transaction.getUserId());

        assertEquals(
                wallet.getId(),
                transaction.getWalletId());

        assertEquals(
                Currency.VES,
                transaction.getCurrency());

        assertEquals(
                TransactionType.REWARD,
                transaction.getTransactionType());

        assertEquals(
                0,
                transaction.getAmount().compareTo(
                        new BigDecimal("1000")));

        assertEquals(
                0,
                transaction.getBalanceBefore().compareTo(
                        BigDecimal.ZERO));

        assertEquals(
                0,
                transaction.getBalanceAfter().compareTo(
                        new BigDecimal("1000")));

        assertEquals(
                "TEST",
                transaction.getSource());

        assertEquals(
                "TEST-LEDGER-001",
                transaction.getReferenceId());

        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.getStatus());

        assertEquals(
                "Test reward credit",
                transaction.getDescription());

        assertNull(transaction.getMetadata());

        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void shouldCreateLedgerEntryWhenWalletIsDebited() {

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("1500"),
                        TransactionType.REWARD,
                        "TEST",
                        "TEST-DEBIT-LEDGER-CREDIT",
                        "Initial test credit",
                        null));

        Wallet wallet = walletService.debitWallet(
                testUserId,
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.WITHDRAWAL,
                        "TEST",
                        "TEST-DEBIT-LEDGER-001",
                        "Test withdrawal debit",
                        null));

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("500")));

        WalletTransaction transaction = walletTransactionRepository
                .findByReferenceId("TEST-DEBIT-LEDGER-001")
                .orElseThrow();

        assertNotNull(transaction.getTransactionId());

        assertTrue(
                transaction.getTransactionId()
                        .startsWith("TXN-"));

        assertEquals(
                testUserId,
                transaction.getUserId());

        assertEquals(
                wallet.getId(),
                transaction.getWalletId());

        assertEquals(
                Currency.VES,
                transaction.getCurrency());

        assertEquals(
                TransactionType.WITHDRAWAL,
                transaction.getTransactionType());

        assertEquals(
                0,
                transaction.getAmount().compareTo(
                        new BigDecimal("1000")));

        assertEquals(
                0,
                transaction.getBalanceBefore().compareTo(
                        new BigDecimal("1500")));

        assertEquals(
                0,
                transaction.getBalanceAfter().compareTo(
                        new BigDecimal("500")));

        assertEquals(
                "TEST",
                transaction.getSource());

        assertEquals(
                "TEST-DEBIT-LEDGER-001",
                transaction.getReferenceId());

        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.getStatus());

        assertEquals(
                "Test withdrawal debit",
                transaction.getDescription());

        assertNull(transaction.getMetadata());

        assertNotNull(transaction.getCreatedAt());
    }
}

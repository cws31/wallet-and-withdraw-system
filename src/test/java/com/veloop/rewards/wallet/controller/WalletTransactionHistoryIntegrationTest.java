package com.veloop.rewards.wallet.controller;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.wallet.service.WalletTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletTransactionHistoryIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Transaction History Test User")
                .email("history-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        walletService.createWallet(savedUser.getId());

        testUserId = savedUser.getId();

        // Credit 5000 VES
        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "HISTORY-CREDIT-" + System.nanoTime(),
                        "History credit test",
                        null));

        // Debit 1000 VES
        walletService.debitWallet(
                testUserId,
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.WITHDRAWAL,
                        "TEST",
                        "HISTORY-DEBIT-" + System.nanoTime(),
                        "History debit test",
                        null));
    }

    @Test
    void shouldReturnUserTransactionHistory() {

        List<WalletTransaction> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();

        assertEquals(2, transactions.size());

        WalletTransaction credit = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.REWARD)
                .findFirst()
                .orElseThrow();

        WalletTransaction debit = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                .findFirst()
                .orElseThrow();

        assertEquals(
                0,
                credit.getAmount()
                        .compareTo(new BigDecimal("5000")));

        assertEquals(
                0,
                debit.getAmount()
                        .compareTo(new BigDecimal("1000")));

        assertEquals(
                0,
                debit.getBalanceBefore()
                        .compareTo(new BigDecimal("5000")));

        assertEquals(
                0,
                debit.getBalanceAfter()
                        .compareTo(new BigDecimal("4000")));
    }

    @Test
    void shouldReturnTransactionsOnlyForRequestedUser() {

        User anotherUser = User.builder()
                .name("Another User")
                .email("another-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedAnotherUser = userRepository.saveAndFlush(anotherUser);

        walletService.createWallet(savedAnotherUser.getId());

        walletService.creditWallet(
                savedAnotherUser.getId(),
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("9000"),
                        TransactionType.BONUS,
                        "TEST",
                        "OTHER-USER-" + System.nanoTime(),
                        "Other user transaction",
                        null));

        List<WalletTransaction> firstUserTransactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();

        assertEquals(2, firstUserTransactions.size());

        assertTrue(
                firstUserTransactions.stream()
                        .noneMatch(transaction -> transaction.getUserId()
                                .equals(savedAnotherUser.getId())));
    }

    @Test
    void shouldSupportPagination() {

        var firstPage = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(0, 1));

        assertEquals(1, firstPage.getContent().size());

        assertEquals(2, firstPage.getTotalElements());

        assertEquals(2, firstPage.getTotalPages());
    }
}
package com.veloop.rewards.wallet.service;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
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
class WalletTransactionServiceTest {

    @Autowired
    private WalletTransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;
    private Long testWalletId;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Transaction Service Test")
                .email("transaction-service-test@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserId(savedUser.getId());

        Wallet savedWallet = walletRepository.save(wallet);

        testUserId = savedUser.getId();
        testWalletId = savedWallet.getId();
    }

    @Test
    void shouldCreateWalletTransaction() {

        WalletTransaction transaction = transactionService.createTransaction(
                testUserId,
                testWalletId,
                Currency.VES,
                TransactionType.REWARD,
                new BigDecimal("500"),
                new BigDecimal("1000"),
                new BigDecimal("1500"),
                "DAILY_REWARD",
                "REWARD-001",
                TransactionStatus.COMPLETED,
                "Daily reward credit",
                "{\"reward\":\"daily-login\"}");

        assertNotNull(transaction);

        assertNotNull(
                transaction.getId());

        assertNotNull(
                transaction.getTransactionId());

        assertTrue(
                transaction.getTransactionId()
                        .startsWith("TXN-"));

        assertEquals(
                testUserId,
                transaction.getUserId());

        assertEquals(
                testWalletId,
                transaction.getWalletId());

        assertEquals(
                Currency.VES,
                transaction.getCurrency());

        assertEquals(
                TransactionType.REWARD,
                transaction.getTransactionType());

        assertEquals(
                new BigDecimal("500"),
                transaction.getAmount());

        assertEquals(
                new BigDecimal("1000"),
                transaction.getBalanceBefore());

        assertEquals(
                new BigDecimal("1500"),
                transaction.getBalanceAfter());

        assertEquals(
                TransactionStatus.COMPLETED,
                transaction.getStatus());

        assertEquals(
                "DAILY_REWARD",
                transaction.getSource());
    }
}
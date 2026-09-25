package com.veloop.rewards.wallet.repository;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WalletTransactionRepositoryTest {

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Transaction Test User")
                .email("transaction-test@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.save(user);

        testUserId = savedUser.getId();

        testWallet = walletService.createWallet(testUserId);
    }

    @Test
    void shouldSaveAndRetrieveWalletTransaction() {

        WalletTransaction transaction = WalletTransaction.create();

        transaction.setTransactionId("TXN-TEST-001");
        transaction.setUserId(testUserId);
        transaction.setWalletId(testWallet.getId());
        transaction.setCurrency(Currency.VES);
        transaction.setTransactionType(
                TransactionType.REWARD);
        transaction.setAmount(
                new BigDecimal("500"));
        transaction.setBalanceBefore(
                new BigDecimal("1000"));
        transaction.setBalanceAfter(
                new BigDecimal("1500"));
        transaction.setSource("DAILY_REWARD");
        transaction.setReferenceId("REWARD-001");
        transaction.setStatus(
                TransactionStatus.COMPLETED);
        transaction.setDescription(
                "Daily reward credit");
        transaction.setMetadata(
                "{\"reward\":\"daily-login\"}");
        transaction.setCreatedAt(
                LocalDateTime.now());

        WalletTransaction saved = transactionRepository.saveAndFlush(
                transaction);

        assertNotNull(saved.getId());

        WalletTransaction retrieved = transactionRepository
                .findByTransactionId(
                        "TXN-TEST-001")
                .orElseThrow();

        assertEquals(
                Currency.VES,
                retrieved.getCurrency());

        assertEquals(
                TransactionType.REWARD,
                retrieved.getTransactionType());

        assertEquals(
                new BigDecimal("500"),
                retrieved.getAmount());

        assertEquals(
                new BigDecimal("1000"),
                retrieved.getBalanceBefore());

        assertEquals(
                new BigDecimal("1500"),
                retrieved.getBalanceAfter());

        assertEquals(
                TransactionStatus.COMPLETED,
                retrieved.getStatus());

        assertEquals(
                "DAILY_REWARD",
                retrieved.getSource());
    }
}
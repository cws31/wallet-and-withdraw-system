package com.veloop.rewards.wallet.service;

import com.veloop.rewards.common.exception.InsufficientBalanceException;
import com.veloop.rewards.common.exception.InvalidAmountException;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
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

        Wallet wallet = walletService.createWallet(
                savedUser.getId());

        testUserId = savedUser.getId();
    }

    @Test
    void shouldCreditVesSuccessfully() {

        Wallet wallet = walletService.creditWallet(
                testUserId,
                Currency.VES,
                new BigDecimal("1000"));

        assertEquals(
                new BigDecimal("1000"),
                wallet.getVes());
    }

    @Test
    void shouldCreditAndDebitVesSuccessfully() {

        walletService.creditWallet(
                testUserId,
                Currency.VES,
                new BigDecimal("1500"));

        Wallet wallet = walletService.debitWallet(
                testUserId,
                Currency.VES,
                new BigDecimal("1000"));

        assertEquals(
                new BigDecimal("500"),
                wallet.getVes());
    }

    @Test
    void shouldRejectDebitWhenBalanceIsInsufficient() {

        walletService.creditWallet(
                testUserId,
                Currency.VES,
                new BigDecimal("500"));

        assertThrows(
                InsufficientBalanceException.class,
                () -> walletService.debitWallet(
                        testUserId,
                        Currency.VES,
                        new BigDecimal("1000")));
    }

    @Test
    void shouldRejectZeroAmount() {

        assertThrows(
                InvalidAmountException.class,
                () -> walletService.creditWallet(
                        testUserId,
                        Currency.VES,
                        BigDecimal.ZERO));
    }

    @Test
    void shouldRejectNegativeAmount() {

        assertThrows(
                InvalidAmountException.class,
                () -> walletService.debitWallet(
                        testUserId,
                        Currency.VES,
                        new BigDecimal("-100")));
    }

    @Test
    void shouldKeepCurrenciesIndependent() {

        walletService.creditWallet(
                testUserId,
                Currency.VES,
                new BigDecimal("1000"));

        walletService.creditWallet(
                testUserId,
                Currency.GEMS,
                new BigDecimal("250"));

        Wallet wallet = walletService.getWallet(testUserId);

        assertEquals(
                new BigDecimal("1000"),
                wallet.getVes());

        assertEquals(
                new BigDecimal("250"),
                wallet.getGems());

        assertEquals(
                BigDecimal.ZERO,
                wallet.getSves());
    }
}
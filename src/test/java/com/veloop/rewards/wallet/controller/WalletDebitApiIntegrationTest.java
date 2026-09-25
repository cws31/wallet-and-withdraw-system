package com.veloop.rewards.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletDebitApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private User admin;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .name("Debit API User")
                .email("debit-user-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        user = userRepository.saveAndFlush(user);

        walletService.createWallet(user.getId());

        admin = User.builder()
                .name("Debit API Admin")
                .email("debit-admin-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("ADMIN")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        admin = userRepository.saveAndFlush(admin);

        walletService.createWallet(admin.getId());

        // Give ADMIN 10,000 VES so debit can be tested.
        walletService.creditWallet(
                admin.getId(),
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("10000"),
                        TransactionType.ADMIN_CREDIT,
                        "TEST",
                        "DEBIT-INITIAL-" + System.nanoTime(),
                        "Initial debit test balance",
                        null));

        userToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole());

        adminToken = jwtService.generateToken(
                admin.getId(),
                admin.getEmail(),
                admin.getRole());
    }

    @Test
    void shouldRejectDebitWithoutJwt() throws Exception {

        WalletDebitRequest request = new WalletDebitRequest(
                Currency.VES,
                new BigDecimal("3000"),
                TransactionType.WITHDRAWAL,
                "TEST",
                "NO-JWT-" + System.nanoTime(),
                "Unauthorized debit",
                null);

        mockMvc.perform(
                post("/api/wallet/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDebitForNormalUser() throws Exception {

        WalletDebitRequest request = new WalletDebitRequest(
                Currency.VES,
                new BigDecimal("3000"),
                TransactionType.WITHDRAWAL,
                "TEST",
                "USER-DEBIT-" + System.nanoTime(),
                "Normal user debit attempt",
                null);

        mockMvc.perform(
                post("/api/wallet/debit")
                        .header(
                                "Authorization",
                                "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Wallet wallet = walletRepository
                .findByUserId(user.getId())
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldAllowDebitForAdmin() throws Exception {

        String referenceId = "ADMIN-DEBIT-" + System.nanoTime();

        WalletDebitRequest request = new WalletDebitRequest(
                Currency.VES,
                new BigDecimal("3000"),
                TransactionType.WITHDRAWAL,
                "ADMIN",
                referenceId,
                "Admin debit test",
                null);

        mockMvc.perform(
                post("/api/wallet/debit")
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.success",
                        is(true)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Wallet debited successfully")))
                .andExpect(jsonPath(
                        "$.data.ves",
                        is(7000.0)));

        Wallet wallet = walletRepository
                .findByUserId(admin.getId())
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("7000")));

        WalletTransaction transaction = transactionRepository
                .findByReferenceId(referenceId)
                .orElseThrow();

        assertEquals(
                admin.getId(),
                transaction.getUserId());

        assertEquals(
                Currency.VES,
                transaction.getCurrency());

        assertEquals(
                TransactionType.WITHDRAWAL,
                transaction.getTransactionType());

        assertEquals(
                0,
                transaction.getAmount()
                        .compareTo(new BigDecimal("3000")));

        assertEquals(
                0,
                transaction.getBalanceBefore()
                        .compareTo(new BigDecimal("10000")));

        assertEquals(
                0,
                transaction.getBalanceAfter()
                        .compareTo(new BigDecimal("7000")));
    }

    @Test
    void shouldRejectDebitWhenBalanceIsInsufficient() throws Exception {

        String referenceId = "INSUFFICIENT-" + System.nanoTime();

        WalletDebitRequest request = new WalletDebitRequest(
                Currency.VES,
                new BigDecimal("15000"),
                TransactionType.WITHDRAWAL,
                "ADMIN",
                referenceId,
                "Insufficient balance test",
                null);

        mockMvc.perform(
                post("/api/wallet/debit")
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Wallet wallet = walletRepository
                .findByUserId(admin.getId())
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("10000")));

        assertTrue(
                transactionRepository
                        .findByReferenceId(referenceId)
                        .isEmpty());
    }

    @Test
    void shouldRejectInvalidDebitAmount() throws Exception {

        WalletDebitRequest request = new WalletDebitRequest(
                Currency.VES,
                BigDecimal.ZERO,
                TransactionType.WITHDRAWAL,
                "ADMIN",
                "INVALID-" + System.nanoTime(),
                "Invalid debit amount",
                null);

        mockMvc.perform(
                post("/api/wallet/debit")
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Wallet wallet = walletRepository
                .findByUserId(admin.getId())
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("10000")));
    }
}
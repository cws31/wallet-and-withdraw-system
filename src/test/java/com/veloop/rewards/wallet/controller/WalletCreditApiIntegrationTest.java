package com.veloop.rewards.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
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
class WalletCreditApiIntegrationTest {

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
                .name("Credit API User")
                .email("credit-user-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        user = userRepository.saveAndFlush(user);

        walletService.createWallet(user.getId());

        admin = User.builder()
                .name("Credit API Admin")
                .email("credit-admin-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("ADMIN")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        admin = userRepository.saveAndFlush(admin);

        walletService.createWallet(admin.getId());

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
    void shouldRejectCreditWithoutJwt() throws Exception {

        WalletCreditRequest request = new WalletCreditRequest(
                Currency.VES,
                new BigDecimal("5000"),
                TransactionType.REWARD,
                "ADMIN",
                "NO-JWT-" + System.nanoTime(),
                "Unauthorized credit",
                null);

        mockMvc.perform(
                post("/api/wallet/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectCreditForNormalUser() throws Exception {

        WalletCreditRequest request = new WalletCreditRequest(
                Currency.VES,
                new BigDecimal("5000"),
                TransactionType.REWARD,
                "ADMIN",
                "USER-CREDIT-" + System.nanoTime(),
                "Normal user credit attempt",
                null);

        mockMvc.perform(
                post("/api/wallet/credit")
                        .header(
                                "Authorization",
                                "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        Wallet wallet = walletRepository
                .findByUserId(user.getId())
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldAllowCreditForAdmin() throws Exception {

        String referenceId = "ADMIN-CREDIT-" + System.nanoTime();

        WalletCreditRequest request = new WalletCreditRequest(
                Currency.VES,
                new BigDecimal("5000"),
                TransactionType.REWARD,
                "ADMIN",
                referenceId,
                "Admin reward credit",
                null);

        mockMvc.perform(
                post("/api/wallet/credit")
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.success",
                        is(true)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Wallet credited successfully")))
                .andExpect(jsonPath(
                        "$.data.ves",
                        is(5000.0)));

        Wallet wallet = walletRepository
                .findByUserId(admin.getId())
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes().compareTo(
                        new BigDecimal("5000")));

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
                0,
                transaction.getAmount()
                        .compareTo(new BigDecimal("5000")));

        assertEquals(
                0,
                transaction.getBalanceAfter()
                        .compareTo(new BigDecimal("5000")));
    }

    @Test
    void shouldRejectInvalidCreditAmount() throws Exception {

        WalletCreditRequest request = new WalletCreditRequest(
                Currency.VES,
                BigDecimal.ZERO,
                TransactionType.REWARD,
                "ADMIN",
                "INVALID-" + System.nanoTime(),
                "Invalid amount",
                null);

        mockMvc.perform(
                post("/api/wallet/credit")
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        Wallet wallet = walletRepository
                .findByUserId(admin.getId())
                .orElseThrow();

        assertTrue(
                wallet.getVes().compareTo(BigDecimal.ZERO) == 0);
    }
}
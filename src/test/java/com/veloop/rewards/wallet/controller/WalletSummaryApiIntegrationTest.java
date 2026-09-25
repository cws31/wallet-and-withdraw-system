package com.veloop.rewards.wallet.controller;

import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletSummaryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Wallet Summary Test User")
                .email("summary-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        walletService.createWallet(savedUser.getId());

        walletService.creditWallet(
                savedUser.getId(),
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "SUMMARY-CREDIT-" + System.nanoTime(),
                        "Summary credit test",
                        null));

        walletService.debitWallet(
                savedUser.getId(),
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.WITHDRAWAL,
                        "TEST",
                        "SUMMARY-DEBIT-" + System.nanoTime(),
                        "Summary debit test",
                        null));

        jwtToken = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole());
    }

    @Test
    void shouldReturnWalletSummaryForAuthenticatedUser()
            throws Exception {

        mockMvc.perform(
                get("/api/wallet/summary")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.success",
                        is(true)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Wallet summary retrieved successfully")))
                .andExpect(jsonPath(
                        "$.data.ves",
                        is(4000.0)))
                .andExpect(jsonPath(
                        "$.data.sves",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.gems",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.tokens",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.spins",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.totalTransactions",
                        is(2)));
    }

    @Test
    void shouldRejectSummaryRequestWithoutJwt()
            throws Exception {

        mockMvc.perform(
                get("/api/wallet/summary"))
                .andExpect(status().isUnauthorized());
    }
}
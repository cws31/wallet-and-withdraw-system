package com.veloop.rewards.wallet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletTransactionApiIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private Long testUserId;
    private String jwtToken;

    @BeforeEach
    void setUp() {

        User user = User.builder()
                .name("Wallet API Test User")
                .email("wallet-api-" + System.nanoTime() + "@example.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        testUserId = savedUser.getId();

        walletService.createWallet(testUserId);

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "API-CREDIT-" + System.nanoTime(),
                        "API transaction history test",
                        null));

        jwtToken = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole());
    }

    @Test
    void shouldGetAuthenticatedUsersTransactions() throws Exception {

        mockMvc.perform(
                get("/api/wallet/transactions")
                        .param("page", "1")
                        .param("limit", "20")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Wallet transactions retrieved successfully")))
                .andExpect(jsonPath(
                        "$.data.content",
                        hasSize(1)))
                .andExpect(jsonPath(
                        "$.data.content[0].currency",
                        is("VES")))
                .andExpect(jsonPath(
                        "$.data.content[0].transactionType",
                        is("REWARD")))
                .andExpect(jsonPath(
                        "$.data.content[0].amount",
                        is(5000.0)));
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {

        mockMvc.perform(
                get("/api/wallet/transactions")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotExposeUserIdInTransactionResponse() throws Exception {

        String response = mockMvc.perform(
                get("/api/wallet/transactions")
                        .param("page", "1")
                        .param("limit", "20")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        JsonNode transaction = json.path("data")
                .path("content")
                .get(0);

        assertNotNull(transaction);

        assertFalse(transaction.has("userId"));
    }
}
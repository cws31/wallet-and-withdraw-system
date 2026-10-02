package com.veloop.rewards.withdrawal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.service.WalletService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WithdrawalApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Autowired
    private PayoutOptionRepository payoutOptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;

    private Wallet wallet;

    private PayoutMethod payoutMethod;

    private PayoutOption payoutOption;

    private String jwtToken;

    @BeforeEach
    void setUp() {

        testUser = User.builder()
                .name("Withdrawal API Test User")
                .email(
                        "withdrawal-api-"
                                + UUID.randomUUID()
                                + "@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(true)
                .level(0)
                .build();

        testUser = userRepository.saveAndFlush(testUser);

        wallet = walletRepository
                .findByUserId(testUser.getId())
                .orElseGet(() -> walletService.createWallet(
                        testUser.getId()));

        payoutMethod = payoutMethodRepository
                .findByCode("UPI")
                .orElseGet(() -> {

                    PayoutMethod method = new PayoutMethod();

                    method.setCode("UPI");
                    method.setName("UPI");
                    method.setActive(true);

                    return payoutMethodRepository
                            .saveAndFlush(method);
                });

        payoutOption = payoutOptionRepository
                .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                        payoutMethod.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> {

                    PayoutOption option = new PayoutOption();

                    option.setMethod(payoutMethod);
                    option.setPayoutAmount(
                            new BigDecimal("10"));
                    option.setCurrency("INR");
                    option.setCurrencyAmount(
                            new BigDecimal("2400"));
                    option.setActive(true);

                    return payoutOptionRepository
                            .saveAndFlush(option);
                });

        WalletCreditRequest creditRequest = new WalletCreditRequest(
                Currency.VES,
                new BigDecimal("5000"),
                TransactionType.REWARD,
                "WITHDRAWAL_API_TEST",
                "WITHDRAWAL_API_TEST",
                "Test VES credit",
                null);

        walletService.creditWallet(
                testUser.getId(),
                creditRequest);

        jwtToken = jwtService.generateToken(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getRole());
    }

    @Test
    void shouldCreateWithdrawalThroughHttpApiWithValidUpi()
            throws Exception {

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "9876543210@ybl"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId());

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "api-test-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isCreated())
                .andExpect(
                        jsonPath(
                                "$.success",
                                is(true)))
                .andExpect(
                        jsonPath(
                                "$.message",
                                is(
                                        "Withdrawal request created successfully")))
                .andExpect(
                        jsonPath(
                                "$.data.withdrawalId")
                                .exists())
                .andExpect(
                        jsonPath(
                                "$.data.status",
                                is("PENDING")));
    }

    @Test
    void shouldRejectInvalidUpiBeforeCreatingWithdrawal()
            throws Exception {

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "invalid-upi"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId());

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "api-invalid-upi-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is("Invalid UPI ID")));

        Wallet currentWallet = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                currentWallet
                        .getVes()
                        .compareTo(new BigDecimal("5000")));
    }

    @Test
    void shouldRejectScriptLikePayoutDetails()
            throws Exception {

        String maliciousPayoutDetails = "<script>alert('xss')</script>";

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "%s"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId(),
                maliciousPayoutDetails);

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "script-details-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is("Invalid UPI ID")));

        Wallet currentWallet = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                currentWallet
                        .getVes()
                        .compareTo(new BigDecimal("5000")));
    }

    @Test
    void shouldRejectBlankPayoutDetailsThroughHttpApi()
            throws Exception {

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "blank-details-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payoutMethodId": %d,
                                  "payoutOptionId": %d,
                                  "payoutDetails": ""
                                }
                                """.formatted(
                                payoutMethod.getId(),
                                payoutOption.getId())))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is("Validation failed")))
                .andExpect(
                        jsonPath(
                                "$.errors.payoutDetails",
                                is("Payout details are required")));
    }

    @Test
    void shouldRejectOversizedPayoutDetails()
            throws Exception {

        String oversizedPayoutDetails = "A".repeat(321);

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "%s"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId(),
                oversizedPayoutDetails);

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "oversized-details-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is("Validation failed")))
                .andExpect(
                        jsonPath(
                                "$.errors.payoutDetails",
                                is(
                                        "Payout details must not exceed 320 characters")));

        Wallet currentWallet = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                currentWallet
                        .getVes()
                        .compareTo(new BigDecimal("5000")));
    }

    @Test
    void shouldRejectWithdrawalWithoutJwt()
            throws Exception {

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "9876543210@ybl"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId());

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                "Idempotency-Key",
                                "api-no-jwt-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isUnauthorized());
    }

    @Test
    void shouldRejectWithdrawalWithoutIdempotencyKey()
            throws Exception {

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "9876543210@ybl"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId());

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is(
                                        "Idempotency-Key header is required")));
    }
}

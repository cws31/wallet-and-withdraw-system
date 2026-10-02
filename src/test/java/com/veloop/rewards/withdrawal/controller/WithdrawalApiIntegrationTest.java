package com.veloop.rewards.withdrawal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
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
import org.springframework.test.web.servlet.MvcResult;

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

        payoutMethod.setActive(true);

        payoutMethod = payoutMethodRepository
                .saveAndFlush(payoutMethod);

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

        payoutOption.setActive(true);

        payoutOption = payoutOptionRepository
                .saveAndFlush(payoutOption);

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
                                HttpHeaders.AUTHORIZATION,
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
                                HttpHeaders.AUTHORIZATION,
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
                        .compareTo(
                                new BigDecimal("5000")));
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
                        .compareTo(
                                new BigDecimal("5000")));
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
                                is(
                                        "Payout details are required")));
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
                        .compareTo(
                                new BigDecimal("5000")));
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
                                HttpHeaders.AUTHORIZATION,
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

    @Test
    void shouldRejectMalformedJwt()
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
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid.jwt.token")
                        .header(
                                "Idempotency-Key",
                                "malformed-jwt-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isUnauthorized());
    }

    @Test
    void shouldRejectWithdrawalWithInsufficientBalance()
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

        walletService.debitWallet(
                testUser.getId(),
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.WITHDRAWAL,
                        "SECURITY_TEST",
                        "SECURITY_TEST",
                        "Prepare insufficient balance test",
                        null));

        Wallet beforeWithdrawal = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                beforeWithdrawal
                        .getVes()
                        .compareTo(BigDecimal.ZERO));

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "insufficient-balance-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest());

        Wallet afterWithdrawal = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                afterWithdrawal
                        .getVes()
                        .compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectInactivePayoutMethod()
            throws Exception {

        payoutMethod.setActive(false);

        payoutMethodRepository.saveAndFlush(
                payoutMethod);

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
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "inactive-method-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is(
                                        "Selected payout method is inactive")));

        Wallet currentWallet = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                currentWallet
                        .getVes()
                        .compareTo(
                                new BigDecimal("5000")));
    }

    @Test
    void shouldRejectInactivePayoutOption()
            throws Exception {

        payoutOption.setActive(false);

        payoutOptionRepository.saveAndFlush(
                payoutOption);

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
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "inactive-option-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.message",
                                is(
                                        "Selected payout option is inactive")));

        Wallet currentWallet = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                currentWallet
                        .getVes()
                        .compareTo(
                                new BigDecimal("5000")));
    }

    @Test
    void shouldReturnSameWithdrawalForSameIdempotencyKey()
            throws Exception {

        String idempotencyKey = "duplicate-request-"
                + UUID.randomUUID();

        String requestBody = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "9876543210@ybl"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId());

        MvcResult firstResult = mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                idempotencyKey)
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isCreated())
                .andReturn();

        MvcResult secondResult = mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                idempotencyKey)
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isCreated())
                .andExpect(
                        jsonPath(
                                "$.success",
                                is(true)))
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(
                firstResult
                        .getResponse()
                        .getContentAsString());

        JsonNode secondJson = objectMapper.readTree(
                secondResult
                        .getResponse()
                        .getContentAsString());

        String firstWithdrawalId = firstJson
                .at("/data/withdrawalId")
                .asText();

        String secondWithdrawalId = secondJson
                .at("/data/withdrawalId")
                .asText();

        assertEquals(
                firstWithdrawalId,
                secondWithdrawalId);

        Wallet currentWallet = walletService.getWallet(
                testUser.getId());

        assertEquals(
                0,
                currentWallet
                        .getVes()
                        .compareTo(
                                new BigDecimal("2600")));
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentRequest()
            throws Exception {

        String idempotencyKey = "fingerprint-mismatch-"
                + UUID.randomUUID();

        String firstRequest = """
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
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                idempotencyKey)
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(
                        status().isCreated());

        String secondRequest = """
                {
                  "payoutMethodId": %d,
                  "payoutOptionId": %d,
                  "payoutDetails": "different@ybl"
                }
                """.formatted(
                payoutMethod.getId(),
                payoutOption.getId());

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                idempotencyKey)
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(secondRequest))
                .andExpect(
                        status().isConflict());
    }

    @Test
    void shouldRejectWithdrawalWhenRateLimitExceeded()
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

        int rateLimit = 5;

        for (int requestNumber = 1; requestNumber <= rateLimit; requestNumber++) {

            mockMvc.perform(
                    post("/api/withdrawals")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + jwtToken)
                            .header(
                                    "Idempotency-Key",
                                    "rate-limit-"
                                            + UUID.randomUUID())
                            .contentType(
                                    MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(
                            status().isBadRequest());
        }

        mockMvc.perform(
                post("/api/withdrawals")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtToken)
                        .header(
                                "Idempotency-Key",
                                "rate-limit-exceeded-"
                                        + UUID.randomUUID())
                        .contentType(
                                MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(
                        status().isTooManyRequests());
    }
}

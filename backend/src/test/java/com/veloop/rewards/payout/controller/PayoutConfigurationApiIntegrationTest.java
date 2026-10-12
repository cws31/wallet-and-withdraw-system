package com.veloop.rewards.payout.controller;

import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PayoutConfigurationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
                .name("Payout Configuration Test User")
                .email("payout-config-" + System.nanoTime() + "@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        jwtToken = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole());
    }

    @Test
    void shouldReturnActivePayoutConfiguration()
            throws Exception {

        mockMvc.perform(
                get("/api/payouts/configuration")
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
                        is("Payout configuration retrieved successfully")))
                .andExpect(jsonPath(
                        "$.data",
                        hasSize(3)));
    }

    @Test
    void shouldReturnUpiWithEightOptions()
            throws Exception {

        mockMvc.perform(
                get("/api/payouts/configuration")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[0].code",
                        is("AMAZON_GIFT_CARD")))
                .andExpect(jsonPath(
                        "$.data[1].code",
                        is("GOOGLE_PLAY_GIFT_CARD")))
                .andExpect(jsonPath(
                        "$.data[2].code",
                        is("UPI")))
                .andExpect(jsonPath(
                        "$.data[2].options",
                        hasSize(8)));
    }

    @Test
    void shouldReturnCorrectUpiPayoutOptions()
            throws Exception {

        mockMvc.perform(
                get("/api/payouts/configuration")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())

                .andExpect(jsonPath(
                        "$.data[2].options[0].payoutAmount",
                        is(10.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[0].currency",
                        is("INR")))
                .andExpect(jsonPath(
                        "$.data[2].options[0].currencyAmount",
                        is(2400.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[1].payoutAmount",
                        is(25.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[1].currencyAmount",
                        is(5800.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[2].payoutAmount",
                        is(50.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[2].currencyAmount",
                        is(10000.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[3].payoutAmount",
                        is(100.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[3].currencyAmount",
                        is(19500.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[4].payoutAmount",
                        is(150.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[4].currencyAmount",
                        is(28500.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[5].payoutAmount",
                        is(300.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[5].currencyAmount",
                        is(52500.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[6].payoutAmount",
                        is(500.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[6].currencyAmount",
                        is(80500.0)))

                .andExpect(jsonPath(
                        "$.data[2].options[7].payoutAmount",
                        is(1000.0)))
                .andExpect(jsonPath(
                        "$.data[2].options[7].currencyAmount",
                        is(150000.0)));
    }

    @Test
    void shouldNotReturnInactivePaypal()
            throws Exception {

        mockMvc.perform(
                get("/api/payouts/configuration")
                        .header(
                                "Authorization",
                                "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.code == 'PAYPAL')]",
                        not(hasSize(1))));
    }

    @Test
    void shouldRejectRequestWithoutJwt()
            throws Exception {

        mockMvc.perform(
                get("/api/payouts/configuration")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
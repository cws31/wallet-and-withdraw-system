package com.veloop.rewards.wallet;

import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WalletJwtIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private JwtService jwtService;

    @Test
    void jwtUserCanOnlyAccessOwnWallet() throws Exception {

        String uniqueId = String.valueOf(System.nanoTime());

        User userA = new User();
        userA.setEmail("jwt-a-" + uniqueId + "@test.com");
        userA.setPasswordHash("test-password");
        userA.setName("User A");

        User savedUserA = userRepository.save(userA);

        User userB = new User();
        userB.setEmail("jwt-b-" + uniqueId + "@test.com");
        userB.setPasswordHash("test-password");
        userB.setName("User B");

        User savedUserB = userRepository.save(userB);

        Wallet walletA = walletService.createWallet(savedUserA.getId());
        Wallet walletB = walletService.createWallet(savedUserB.getId());

        walletService.creditWallet(
                savedUserB.getId(),
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "JWT-ISOLATION-" + uniqueId,
                        "JWT isolation test",
                        null));

        String tokenA = jwtService.generateToken(
                savedUserA.getId(),
                savedUserA.getEmail(),
                savedUserA.getRole());

        mockMvc.perform(
                get("/api/wallet")
                        .header(
                                "Authorization",
                                "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.data.ves").value(0.0))

                .andExpect(jsonPath("$.data.userId").doesNotExist());
    }
}
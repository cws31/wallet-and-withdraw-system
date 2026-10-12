package com.veloop.rewards.reconciliation.controller;

import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.repository.WalletRepository;
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
class ReconciliationControllerApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User user;
    private User admin;

    private Wallet userWallet;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .name("Reconciliation Test User")
                .email(
                        "reconciliation-user-"
                                + System.nanoTime()
                                + "@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        user = userRepository.saveAndFlush(user);

        userWallet = walletService.createWallet(user.getId());

        admin = User.builder()
                .name("Reconciliation Test Admin")
                .email(
                        "reconciliation-admin-"
                                + System.nanoTime()
                                + "@example.com")
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
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
    void shouldRejectReconciliationWithoutJwt()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/admin/reconciliation/wallet/"
                                + userWallet.getId())
                        .param(
                                "currency",
                                Currency.VES.name())
                        .contentType(
                                MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectReconciliationForNormalUser()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/admin/reconciliation/wallet/"
                                + userWallet.getId())
                        .param(
                                "currency",
                                Currency.VES.name())
                        .header(
                                "Authorization",
                                "Bearer " + userToken)
                        .contentType(
                                MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowReconciliationForAdmin()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/admin/reconciliation/wallet/"
                                + userWallet.getId())
                        .param(
                                "currency",
                                Currency.VES.name())
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(
                                MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.success",
                        is(true)))
                .andExpect(jsonPath(
                        "$.data.walletId",
                        is(userWallet.getId().intValue())))
                .andExpect(jsonPath(
                        "$.data.userId",
                        is(user.getId().intValue())))
                .andExpect(jsonPath(
                        "$.data.currency",
                        is("VES")))
                .andExpect(jsonPath(
                        "$.data.walletBalance",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.ledgerDerivedBalance",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.difference",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.reconciled",
                        is(true)));
    }

    @Test
    void shouldDetectWalletLedgerMismatch()
            throws Exception {

        userWallet.setVes(
                new BigDecimal("1000"));

        userWallet = walletRepository.saveAndFlush(userWallet);

        mockMvc.perform(
                get(
                        "/api/admin/reconciliation/wallet/"
                                + userWallet.getId())
                        .param(
                                "currency",
                                Currency.VES.name())
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(
                                MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.success",
                        is(true)))
                .andExpect(jsonPath(
                        "$.data.walletBalance",
                        is(1000.0)))
                .andExpect(jsonPath(
                        "$.data.ledgerDerivedBalance",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.difference",
                        is(1000.0)))
                .andExpect(jsonPath(
                        "$.data.reconciled",
                        is(false)))
                .andExpect(jsonPath(
                        "$.message",
                        is(
                                "Wallet reconciliation detected a balance mismatch")));
    }

    @Test
    void shouldReconcileUsingRequestedCurrency()
            throws Exception {

        userWallet.setGems(
                new BigDecimal("2500"));

        userWallet = walletRepository.saveAndFlush(userWallet);

        mockMvc.perform(
                get(
                        "/api/admin/reconciliation/wallet/"
                                + userWallet.getId())
                        .param(
                                "currency",
                                Currency.GEMS.name())
                        .header(
                                "Authorization",
                                "Bearer " + adminToken)
                        .contentType(
                                MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.currency",
                        is("GEMS")))
                .andExpect(jsonPath(
                        "$.data.walletBalance",
                        is(2500.0)))
                .andExpect(jsonPath(
                        "$.data.ledgerDerivedBalance",
                        is(0.0)))
                .andExpect(jsonPath(
                        "$.data.difference",
                        is(2500.0)))
                .andExpect(jsonPath(
                        "$.data.reconciled",
                        is(false)));
    }
}
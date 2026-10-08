
package com.veloop.rewards.demo.config;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "veloop.demo-data.enabled", havingValue = "true", matchIfMissing = false)
public class DemoDataInitializer implements CommandLineRunner {

    private static final String DEMO_EMAIL = "demo@veloop.test";
    private static final String DEMO_PASSWORD = "Demo@12345";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Override
    @Transactional
    public void run(String... args) {

        /*
         * Prevent duplicate demo data.
         */
        if (userRepository.findByEmail(DEMO_EMAIL).isPresent()) {
            System.out.println(
                    "Demo user already exists. Skipping demo data initialization.");
            return;
        }

        User demoUser = User.builder()
                .name("Demo User")
                .email(DEMO_EMAIL)
                .passwordHash(
                        passwordEncoder.encode(DEMO_PASSWORD))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(true)
                .level(1)
                .build();

        User savedUser = userRepository.saveAndFlush(demoUser);

        Long userId = savedUser.getId();

        walletService.createWallet(userId);

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("10000"),
                        TransactionType.AD_REWARD,
                        "DEMO",
                        "DEMO-AD-001",
                        "Demo Ad Reward",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.DAILY_REWARD,
                        "DEMO",
                        "DEMO-DAILY-001",
                        "Demo Daily Reward",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REFERRAL,
                        "DEMO",
                        "DEMO-REFERRAL-001",
                        "Demo Referral Reward",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("6000"),
                        TransactionType.BONUS,
                        "DEMO",
                        "DEMO-BONUS-001",
                        "Demo Bonus",
                        null));

        walletService.debitWallet(
                userId,
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.WITHDRAWAL,
                        "DEMO",
                        "DEMO-WITHDRAWAL-001",
                        "Demo Withdrawal",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.SVES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "DEMO",
                        "DEMO-SVES-001",
                        "Demo SVES Reward",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.GEMS,
                        new BigDecimal("100"),
                        TransactionType.GAME_REWARD,
                        "DEMO",
                        "DEMO-GEMS-001",
                        "Demo Gems Reward",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.TOKENS,
                        new BigDecimal("500"),
                        TransactionType.GAME_REWARD,
                        "DEMO",
                        "DEMO-TOKENS-001",
                        "Demo Tokens Reward",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.SPINS,
                        new BigDecimal("3"),
                        TransactionType.BONUS,
                        "DEMO",
                        "DEMO-SPINS-001",
                        "Demo Spins Bonus",
                        null));

        System.out.println(
                "=================================================");
        System.out.println(
                "VELOOP DEMO DATA INITIALIZED");
        System.out.println(
                "Demo Email    : " + DEMO_EMAIL);
        System.out.println(
                "Demo Password : " + DEMO_PASSWORD);
        System.out.println(
                "VES           : 25,000");
        System.out.println(
                "SVES          : 5,000");
        System.out.println(
                "GEMS          : 100");
        System.out.println(
                "TOKENS        : 500");
        System.out.println(
                "SPINS         : 3");
        System.out.println(
                "=================================================");
    }
}

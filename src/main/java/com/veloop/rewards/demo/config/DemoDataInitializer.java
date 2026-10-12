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
import java.util.List;


@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "veloop.demo-data.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class DemoDataInitializer implements CommandLineRunner {

    private static final String DEMO_SOURCE = "DEMO_SEED";
    private static final String DEMO_PASSWORD = "Demo@12345";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;

    @Override
    @Transactional
    public void run(String... args) {
        List<DemoAccount> users = List.of(
                new DemoAccount("Demo User", "demo@veloop.test", "USER", DEMO_PASSWORD, 26000, 5000, 100, 500, 3),
                new DemoAccount("Demo User 02", "demo.user02@veloop.test", "USER", DEMO_PASSWORD, 12000, 1000, 25, 50, 1),
                new DemoAccount("Demo User 03", "demo.user03@veloop.test", "USER", DEMO_PASSWORD, 18500, 2500, 40, 100, 2),
                new DemoAccount("Demo User 04", "demo.user04@veloop.test", "USER", DEMO_PASSWORD, 7500, 500, 10, 25, 0),
                new DemoAccount("Demo User 05", "demo.user05@veloop.test", "USER", DEMO_PASSWORD, 32000, 7000, 150, 300, 5),
                new DemoAccount("Demo User 06", "demo.user06@veloop.test", "USER", DEMO_PASSWORD, 9400, 1200, 20, 60, 1),
                new DemoAccount("Demo User 07", "demo.user07@veloop.test", "USER", DEMO_PASSWORD, 22000, 3500, 75, 150, 3),
                new DemoAccount("Demo User 08", "demo.user08@veloop.test", "USER", DEMO_PASSWORD, 5100, 300, 5, 10, 0),
                new DemoAccount("Demo User 09", "demo.user09@veloop.test", "USER", DEMO_PASSWORD, 14500, 1800, 35, 80, 2),
                new DemoAccount("Demo User 10", "demo.user10@veloop.test", "USER", DEMO_PASSWORD, 27500, 4500, 90, 200, 4)
        );

        int createdUsers = 0;
        int skippedUsers = 0;
        for (DemoAccount account : users) {
            if (seedAccount(account)) {
                createdUsers++;
            } else {
                skippedUsers++;
            }
        }

        DemoAccount admin = new DemoAccount(
                "VELOop Demo Admin",
                "admin@veloop.test",
                "ADMIN",
                ADMIN_PASSWORD,
                0, 0, 0, 0, 0);
        boolean adminCreated = seedAccount(admin);

        System.out.println("VELOop development demo-data initialization finished.");
        System.out.println("New user accounts created: " + createdUsers);
        System.out.println("Existing user accounts preserved/skipped: " + skippedUsers);
        System.out.println("Admin account: " + (adminCreated ? "created" : "already exists; preserved"));
        System.out.println("Demo users: demo@veloop.test and demo.user02@veloop.test through demo.user10@veloop.test");
        System.out.println("Demo user password (new accounts only): " + DEMO_PASSWORD);
        System.out.println("Admin login: admin@veloop.test / " + ADMIN_PASSWORD);
        System.out.println("IMPORTANT: development/test credentials only; do not enable demo data in production.");
    }

   
    private boolean seedAccount(DemoAccount account) {
        if (userRepository.existsByEmail(account.email())) {
            return false;
        }

        User user = User.builder()
                .name(account.name())
                .email(account.email())
                .passwordHash(passwordEncoder.encode(account.password()))
                .role(account.role())
                .accountStatus("ACTIVE")
                .verified(true)
                .level(1)
                .build();

        User savedUser = userRepository.saveAndFlush(user);
        Long userId = savedUser.getId();
        walletService.createWallet(userId);

     
        if ("USER".equals(account.role())) {
            seedUserWallet(account, userId);
        }
        return true;
    }

    private void seedUserWallet(DemoAccount account, Long userId) {
        String suffix = account.email().replaceAll("[^A-Za-z0-9]", "-").toUpperCase();

      
        if ("demo@veloop.test".equals(account.email())) {
        
            credit(userId, Currency.VES, 10000, TransactionType.AD_REWARD, suffix + "-VES-AD", "Demo ad reward");
            credit(userId, Currency.VES, 5000, TransactionType.DAILY_REWARD, suffix + "-VES-DAILY", "Demo daily reward");
            credit(userId, Currency.VES, 5000, TransactionType.REFERRAL, suffix + "-VES-REFERRAL", "Demo referral reward");
            credit(userId, Currency.VES, 6000, TransactionType.BONUS, suffix + "-VES-BONUS", "Demo bonus");
        } else {
            credit(userId, Currency.VES, account.ves(), TransactionType.AD_REWARD, suffix + "-VES-AD", "Demo ad reward");
            credit(userId, Currency.VES, account.ves() / 4, TransactionType.DAILY_REWARD, suffix + "-VES-DAILY", "Demo daily reward");
        }
        credit(userId, Currency.SVES, account.sves(), TransactionType.REWARD, suffix + "-SVES", "Demo SVES reward");
        credit(userId, Currency.GEMS, account.gems(), TransactionType.GAME_REWARD, suffix + "-GEMS", "Demo gems reward");
        credit(userId, Currency.TOKENS, account.tokens(), TransactionType.GAME_REWARD, suffix + "-TOKENS", "Demo tokens reward");
        credit(userId, Currency.SPINS, account.spins(), TransactionType.BONUS, suffix + "-SPINS", "Demo spins bonus");


        if ("demo@veloop.test".equals(account.email())) {
            walletService.debitWallet(userId, new WalletDebitRequest(
                    Currency.VES, new BigDecimal("1000"), TransactionType.WITHDRAWAL,
                    DEMO_SOURCE, suffix + "-WITHDRAWAL", "Demo withdrawal", null));
        }
    }

    private void credit(Long userId, Currency currency, long amount,
                        TransactionType type, String reference, String description) {
        if (amount <= 0) {
            return;
        }
        walletService.creditWallet(userId, new WalletCreditRequest(
                currency, BigDecimal.valueOf(amount), type,
                DEMO_SOURCE, reference, description, null));
    }

    private record DemoAccount(
            String name,
            String email,
            String role,
            String password,
            long ves,
            long sves,
            long gems,
            long tokens,
            long spins) {
    }
}

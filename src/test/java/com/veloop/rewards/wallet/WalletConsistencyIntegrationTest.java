package com.veloop.rewards.wallet;

import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WalletConsistencyIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void walletShouldMaintainCurrencyIsolationAndLedgerConsistency() {

        Long userId = createTestUserAndGetId();

        Wallet initialWallet = walletService.getWallet(userId);

        assertEquals(
                0,
                initialWallet.getVes().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                initialWallet.getSves().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                initialWallet.getGems().compareTo(BigDecimal.ZERO));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "CONSISTENCY-VES",
                        "VES credit",
                        null));

        Wallet afterVesCredit = walletService.getWallet(userId);

        assertEquals(
                0,
                afterVesCredit.getVes().compareTo(
                        new BigDecimal("5000")));

        assertEquals(
                0,
                afterVesCredit.getSves().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                afterVesCredit.getGems().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                afterVesCredit.getTokens().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                afterVesCredit.getSpins().compareTo(BigDecimal.ZERO));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.GEMS,
                        new BigDecimal("250"),
                        TransactionType.BONUS,
                        "TEST",
                        "CONSISTENCY-GEMS",
                        "Gems credit",
                        null));

        Wallet afterGemsCredit = walletService.getWallet(userId);

        assertEquals(
                0,
                afterGemsCredit.getVes().compareTo(
                        new BigDecimal("5000")));

        assertEquals(
                0,
                afterGemsCredit.getGems().compareTo(
                        new BigDecimal("250")));

        walletService.debitWallet(
                userId,
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.ADMIN_DEBIT,
                        "TEST",
                        "CONSISTENCY-DEBIT",
                        "VES debit",
                        null));

        Wallet afterDebit = walletService.getWallet(userId);

        assertEquals(
                0,
                afterDebit.getVes().compareTo(
                        new BigDecimal("4000")));

        assertEquals(
                0,
                afterDebit.getGems().compareTo(
                        new BigDecimal("250")));

        assertThrows(
                RuntimeException.class,
                () -> walletService.debitWallet(
                        userId,
                        new WalletDebitRequest(
                                Currency.VES,
                                new BigDecimal("5000"),
                                TransactionType.ADMIN_DEBIT,
                                "TEST",
                                "CONSISTENCY-INVALID-DEBIT",
                                "Should fail",
                                null)));

        Wallet afterFailedDebit = walletService.getWallet(userId);

        assertEquals(
                0,
                afterFailedDebit.getVes().compareTo(
                        new BigDecimal("4000")));

        assertEquals(
                0,
                afterFailedDebit.getGems().compareTo(
                        new BigDecimal("250")));

        List<WalletTransaction> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId,
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent();

        assertEquals(3, transactions.size());

        long creditCount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.REWARD)
                .count();

        long gemsCreditCount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BONUS)
                .count();

        long debitCount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.ADMIN_DEBIT)
                .count();

        assertEquals(1, creditCount);
        assertEquals(1, gemsCreditCount);
        assertEquals(1, debitCount);

        WalletTransaction vesCredit = transactions.stream()
                .filter(t -> "CONSISTENCY-VES".equals(t.getReferenceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(Currency.VES, vesCredit.getCurrency());
        assertEquals(
                0,
                vesCredit.getBalanceBefore().compareTo(BigDecimal.ZERO));
        assertEquals(
                0,
                vesCredit.getBalanceAfter().compareTo(
                        new BigDecimal("5000")));

        WalletTransaction gemsCredit = transactions.stream()
                .filter(t -> "CONSISTENCY-GEMS".equals(t.getReferenceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(Currency.GEMS, gemsCredit.getCurrency());
        assertEquals(
                0,
                gemsCredit.getBalanceBefore().compareTo(BigDecimal.ZERO));
        assertEquals(
                0,
                gemsCredit.getBalanceAfter().compareTo(
                        new BigDecimal("250")));

        WalletTransaction vesDebit = transactions.stream()
                .filter(t -> "CONSISTENCY-DEBIT".equals(t.getReferenceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(Currency.VES, vesDebit.getCurrency());
        assertEquals(
                0,
                vesDebit.getBalanceBefore().compareTo(
                        new BigDecimal("5000")));
        assertEquals(
                0,
                vesDebit.getBalanceAfter().compareTo(
                        new BigDecimal("4000")));
    }

    private Long createTestUserAndGetId() {

        String uniqueId = String.valueOf(System.nanoTime());

        User user = new User();
        user.setEmail("consistency-" + uniqueId + "@test.com");
        user.setPasswordHash("test-password");
        user.setName("Consistency Test User");

        User savedUser = userRepository.save(user);

        walletService.createWallet(savedUser.getId());

        return savedUser.getId();
    }
}
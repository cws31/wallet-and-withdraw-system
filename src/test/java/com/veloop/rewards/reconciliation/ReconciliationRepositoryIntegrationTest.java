package com.veloop.rewards.reconciliation;

import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class ReconciliationRepositoryIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCalculateLedgerDerivedBalanceFromCompletedTransactions() {

        Long userId = createTestUserAndGetId();

        Wallet wallet = walletService.getWallet(userId);

        Long walletId = wallet.getId();

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "RECON-VES-CREDIT",
                        "Reconciliation VES credit",
                        null));

        walletService.debitWallet(
                userId,
                new WalletDebitRequest(
                        Currency.VES,
                        new BigDecimal("1000"),
                        TransactionType.ADMIN_DEBIT,
                        "TEST",
                        "RECON-VES-DEBIT",
                        "Reconciliation VES debit",
                        null));

        BigDecimal ledgerDerivedBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        walletId,
                        Currency.VES,
                        TransactionStatus.COMPLETED);

        assertEquals(
                0,
                ledgerDerivedBalance.compareTo(
                        new BigDecimal("4000")));
    }

    @Test
    void shouldKeepCurrenciesSeparatedDuringReconciliation() {

        Long userId = createTestUserAndGetId();

        Wallet wallet = walletService.getWallet(userId);

        Long walletId = wallet.getId();

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "RECON-ISOLATION-VES",
                        "VES test credit",
                        null));

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.GEMS,
                        new BigDecimal("250"),
                        TransactionType.BONUS,
                        "TEST",
                        "RECON-ISOLATION-GEMS",
                        "Gems test credit",
                        null));

        BigDecimal vesLedgerBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        walletId,
                        Currency.VES,
                        TransactionStatus.COMPLETED);

        BigDecimal gemsLedgerBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        walletId,
                        Currency.GEMS,
                        TransactionStatus.COMPLETED);

        assertEquals(
                0,
                vesLedgerBalance.compareTo(
                        new BigDecimal("5000")));

        assertEquals(
                0,
                gemsLedgerBalance.compareTo(
                        new BigDecimal("250")));
    }

    @Test
    void shouldIgnoreNonCompletedTransactions() {

        Long userId = createTestUserAndGetId();

        Wallet wallet = walletService.getWallet(userId);

        Long walletId = wallet.getId();

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "RECON-COMPLETED",
                        "Completed transaction",
                        null));

        BigDecimal completedBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        walletId,
                        Currency.VES,
                        TransactionStatus.COMPLETED);

        BigDecimal pendingBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        walletId,
                        Currency.VES,
                        TransactionStatus.PENDING);

        assertEquals(
                0,
                completedBalance.compareTo(
                        new BigDecimal("5000")));

        assertEquals(
                0,
                pendingBalance.compareTo(
                        BigDecimal.ZERO));
    }

    @Test
    void shouldReturnZeroWhenNoCompletedTransactionsExist() {

        Long userId = createTestUserAndGetId();

        Wallet wallet = walletService.getWallet(userId);

        BigDecimal ledgerDerivedBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        wallet.getId(),
                        Currency.VES,
                        TransactionStatus.COMPLETED);

        assertEquals(
                0,
                ledgerDerivedBalance.compareTo(
                        BigDecimal.ZERO));
    }

    private Long createTestUserAndGetId() {

        String uniqueId = String.valueOf(System.nanoTime());

        User user = new User();

        user.setEmail(
                "reconciliation-" + uniqueId + "@test.com");

        user.setPasswordHash("test-password");

        user.setName("Reconciliation Test User");

        User savedUser = userRepository.save(user);

        walletService.createWallet(
                savedUser.getId());

        return savedUser.getId();
    }
}
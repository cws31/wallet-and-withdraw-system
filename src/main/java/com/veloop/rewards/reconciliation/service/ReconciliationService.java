package com.veloop.rewards.reconciliation.service;

import com.veloop.rewards.audit.service.AuditLogService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.reconciliation.dto.ReconciliationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ReconciliationService {

    private static final String RECONCILIATION_FAILURE = "RECONCILIATION_FAILURE";

    private static final String TARGET_TYPE = "WALLET_RECONCILIATION";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ReconciliationService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {

        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReconciliationResult reconcile(
            Long walletId,
            Currency currency) {

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wallet not found: " + walletId));

        BigDecimal walletBalance = getWalletBalance(wallet, currency);

        BigDecimal ledgerDerivedBalance = walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        walletId,
                        currency,
                        TransactionStatus.COMPLETED);

        if (ledgerDerivedBalance == null) {
            ledgerDerivedBalance = BigDecimal.ZERO;
        }

        BigDecimal difference = walletBalance.subtract(ledgerDerivedBalance);

        boolean reconciled = difference.compareTo(BigDecimal.ZERO) == 0;

        if (!reconciled) {
            recordReconciliationFailure(
                    wallet,
                    currency,
                    walletBalance,
                    ledgerDerivedBalance,
                    difference);
        }

        return new ReconciliationResult(
                wallet.getId(),
                wallet.getUserId(),
                currency,
                walletBalance,
                ledgerDerivedBalance,
                difference,
                reconciled);
    }

    private BigDecimal getWalletBalance(
            Wallet wallet,
            Currency currency) {

        return switch (currency) {
            case VES -> wallet.getVes();
            case SVES -> wallet.getSves();
            case GEMS -> wallet.getGems();
            case TOKENS -> wallet.getTokens();
            case SPINS -> wallet.getSpins();
        };
    }

    private void recordReconciliationFailure(
            Wallet wallet,
            Currency currency,
            BigDecimal walletBalance,
            BigDecimal ledgerDerivedBalance,
            BigDecimal difference) {

        User targetUser = userRepository.findById(wallet.getUserId())
                .orElse(null);

        String metadata = """
                {
                  "walletId": %d,
                  "currency": "%s",
                  "walletBalance": "%s",
                  "ledgerDerivedBalance": "%s",
                  "difference": "%s"
                }
                """.formatted(
                wallet.getId(),
                currency,
                walletBalance,
                ledgerDerivedBalance,
                difference);

        auditLogService.record(
                null,
                targetUser,
                TARGET_TYPE,
                RECONCILIATION_FAILURE,
                String.valueOf(wallet.getId()),
                metadata);
    }
}
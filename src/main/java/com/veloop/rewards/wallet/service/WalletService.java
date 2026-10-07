package com.veloop.rewards.wallet.service;

import com.veloop.rewards.audit.service.AuditLogService;
import com.veloop.rewards.common.exception.InsufficientBalanceException;
import com.veloop.rewards.common.exception.InvalidAmountException;
import com.veloop.rewards.common.exception.WalletAlreadyExistsException;
import com.veloop.rewards.common.exception.WalletNotFoundException;
import com.veloop.rewards.observability.ObservabilityMetrics;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.dto.WalletSummaryResponse;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

        private static final String WITHDRAWAL_REJECTED = "WITHDRAWAL_REJECTED";

        private static final String WITHDRAWAL_CANCELLED = "WITHDRAWAL_CANCELLED";

        private final WalletRepository walletRepository;

        private final WalletTransactionService walletTransactionService;

        private final AuditLogService auditLogService;

        private final UserRepository userRepository;

        private final ObservabilityMetrics observabilityMetrics;

        public WalletService(
                        WalletRepository walletRepository,
                        WalletTransactionService walletTransactionService,
                        AuditLogService auditLogService,
                        UserRepository userRepository,
                        ObservabilityMetrics observabilityMetrics) {

                this.walletRepository = walletRepository;
                this.walletTransactionService = walletTransactionService;
                this.auditLogService = auditLogService;
                this.userRepository = userRepository;
                this.observabilityMetrics = observabilityMetrics;
        }

        @Transactional
        public Wallet createWallet(Long userId) {

                if (walletRepository.existsByUserId(userId)) {
                        throw new WalletAlreadyExistsException(userId);
                }

                Wallet wallet = new Wallet();
                wallet.setUserId(userId);

                return walletRepository.save(wallet);
        }

        @Transactional(readOnly = true)
        public Wallet getWallet(Long userId) {

                return walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new WalletNotFoundException(userId));
        }

        @Transactional
        public Wallet creditWallet(
                        Long userId,
                        WalletCreditRequest request) {

                try {

                        if (request == null) {
                                throw new InvalidAmountException();
                        }

                        Currency currency = request.currency();
                        BigDecimal amount = request.amount();

                        if (currency == null
                                        || amount == null
                                        || amount.compareTo(BigDecimal.ZERO) <= 0) {

                                throw new InvalidAmountException();
                        }

                        Wallet wallet = getWallet(userId);

                        BigDecimal balanceBefore = getBalance(wallet, currency);

                        BigDecimal balanceAfter = balanceBefore.add(amount);

                        setBalance(
                                        wallet,
                                        currency,
                                        balanceAfter);

                        if (currency == Currency.VES
                                        && request.transactionType() == TransactionType.CORRECTION
                                        && isWithdrawalReversal(request.source())) {

                                decreaseWithdrawnVes(
                                                wallet,
                                                amount);
                        }

                        Wallet savedWallet = walletRepository.save(wallet);

                        walletTransactionService.createTransaction(
                                        userId,
                                        savedWallet.getId(),
                                        currency,
                                        request.transactionType(),
                                        amount,
                                        balanceBefore,
                                        balanceAfter,
                                        request.source(),
                                        request.referenceId(),
                                        TransactionStatus.COMPLETED,
                                        request.description(),
                                        request.metadata());

                        User targetUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "User not found for wallet audit: "
                                                                        + userId));

                        auditLogService.record(
                                        null,
                                        targetUser,
                                        "WALLET",
                                        "WALLET_CREDIT",
                                        request.referenceId(),
                                        "currency=" + currency.name()
                                                        + ", amount=" + amount
                                                        + ", balanceBefore="
                                                        + balanceBefore
                                                        + ", balanceAfter="
                                                        + balanceAfter);

                        observabilityMetrics.recordWalletCreditSuccess();

                        return savedWallet;

                } catch (RuntimeException exception) {

                        observabilityMetrics.recordWalletCreditFailure();

                        throw exception;
                }
        }

        @Transactional
        public Wallet debitWallet(
                        Long userId,
                        WalletDebitRequest request) {

                try {

                        if (request == null) {
                                throw new InvalidAmountException();
                        }

                        Currency currency = request.currency();
                        BigDecimal amount = request.amount();

                        if (currency == null
                                        || amount == null
                                        || amount.compareTo(BigDecimal.ZERO) <= 0) {

                                throw new InvalidAmountException();
                        }

                        Wallet wallet = getWallet(userId);

                        BigDecimal balanceBefore = getBalance(wallet, currency);

                        validateBalance(
                                        currency,
                                        balanceBefore,
                                        amount);

                        BigDecimal balanceAfter = balanceBefore.subtract(amount);

                        setBalance(
                                        wallet,
                                        currency,
                                        balanceAfter);

                        if (currency == Currency.VES
                                        && request.transactionType() == TransactionType.WITHDRAWAL) {

                                increaseWithdrawnVes(
                                                wallet,
                                                amount);
                        }

                        Wallet savedWallet = walletRepository.save(wallet);

                        walletTransactionService.createTransaction(
                                        userId,
                                        savedWallet.getId(),
                                        currency,
                                        request.transactionType(),
                                        amount,
                                        balanceBefore,
                                        balanceAfter,
                                        request.source(),
                                        request.referenceId(),
                                        TransactionStatus.COMPLETED,
                                        request.description(),
                                        request.metadata());

                        User targetUser = userRepository.findById(userId)
                                        .orElseThrow(() -> new IllegalStateException(
                                                        "User not found for wallet audit: "
                                                                        + userId));

                        auditLogService.record(
                                        null,
                                        targetUser,
                                        "WALLET",
                                        "WALLET_DEBIT",
                                        request.referenceId(),
                                        "currency=" + currency.name()
                                                        + ", amount=" + amount
                                                        + ", balanceBefore="
                                                        + balanceBefore
                                                        + ", balanceAfter="
                                                        + balanceAfter);

                        observabilityMetrics.recordWalletDebitSuccess();

                        return savedWallet;

                } catch (RuntimeException exception) {

                        observabilityMetrics.recordWalletDebitFailure();

                        throw exception;
                }
        }

        private void increaseWithdrawnVes(
                        Wallet wallet,
                        BigDecimal amount) {

                BigDecimal current = wallet.getWithdrawnVes() == null
                                ? BigDecimal.ZERO
                                : wallet.getWithdrawnVes();

                wallet.setWithdrawnVes(
                                current.add(amount));
        }

        private void decreaseWithdrawnVes(
                        Wallet wallet,
                        BigDecimal amount) {

                BigDecimal current = wallet.getWithdrawnVes() == null
                                ? BigDecimal.ZERO
                                : wallet.getWithdrawnVes();

                BigDecimal updated = current.subtract(amount);

                if (updated.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalStateException(
                                        "withdrawnVes cannot become negative");
                }

                wallet.setWithdrawnVes(updated);
        }

        private boolean isWithdrawalReversal(
                        String source) {

                return WITHDRAWAL_REJECTED.equals(source)
                                || WITHDRAWAL_CANCELLED.equals(source);
        }

        private void validateBalance(
                        Currency currency,
                        BigDecimal available,
                        BigDecimal requested) {

                if (available.compareTo(requested) < 0) {
                        throw new InsufficientBalanceException(
                                        currency.name(),
                                        available,
                                        requested);
                }
        }

        private BigDecimal getBalance(
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

        private void setBalance(
                        Wallet wallet,
                        Currency currency,
                        BigDecimal balance) {

                switch (currency) {
                        case VES -> wallet.setVes(balance);
                        case SVES -> wallet.setSves(balance);
                        case GEMS -> wallet.setGems(balance);
                        case TOKENS -> wallet.setTokens(balance);
                        case SPINS -> wallet.setSpins(balance);
                }
        }

        @Transactional(readOnly = true)
        public WalletSummaryResponse getWalletSummary(
                        Long userId) {

                Wallet wallet = getWallet(userId);

                long totalTransactions = walletTransactionService
                                .countTransactions(userId);

                return new WalletSummaryResponse(
                                wallet.getVes(),
                                wallet.getSves(),
                                wallet.getGems(),
                                wallet.getTokens(),
                                wallet.getSpins(),
                                totalTransactions);
        }
}
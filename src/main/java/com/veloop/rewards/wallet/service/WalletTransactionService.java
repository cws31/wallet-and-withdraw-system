package com.veloop.rewards.wallet.service;

import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veloop.rewards.wallet.dto.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletTransactionService {

    private final WalletTransactionRepository transactionRepository;

    public WalletTransactionService(
            WalletTransactionRepository transactionRepository) {

        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletTransaction createTransaction(
            Long userId,
            Long walletId,
            Currency currency,
            TransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String source,
            String referenceId,
            TransactionStatus status,
            String description,
            String metadata) {

        WalletTransaction transaction = WalletTransaction.create();

        transaction.setTransactionId(
                generateTransactionId());

        transaction.setUserId(userId);
        transaction.setWalletId(walletId);
        transaction.setCurrency(currency);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setSource(source);
        transaction.setReferenceId(referenceId);
        transaction.setStatus(status);
        transaction.setDescription(description);
        transaction.setMetadata(metadata);
        transaction.setCreatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    private String generateTransactionId() {

        return "TXN-" + UUID.randomUUID();
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactions(
            Long userId,
            Pageable pageable) {

        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(WalletTransactionResponse::from);
    }
}

package com.veloop.rewards.wallet.repository;

import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface WalletTransactionRepository
                extends JpaRepository<WalletTransaction, Long> {

        Optional<WalletTransaction> findByTransactionId(
                        String transactionId);

        boolean existsByTransactionId(
                        String transactionId);

        Optional<WalletTransaction> findByReferenceId(
                        String referenceId);

        Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(
                        Long userId,
                        Pageable pageable);

        long countByUserId(
                        Long userId);

        long countByUserIdAndCreatedAtAfter(
                        Long userId,
                        LocalDateTime createdAt);

        long countByUserIdAndStatusAndCreatedAtAfter(
                        Long userId,
                        TransactionStatus status,
                        LocalDateTime createdAt);

        @Query("""
                        SELECT COALESCE(SUM(t.balanceAfter - t.balanceBefore), 0)
                        FROM WalletTransaction t
                        WHERE t.walletId = :walletId
                          AND t.currency = :currency
                          AND t.status = :status
                        """)
        BigDecimal sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        @Param("walletId") Long walletId,
                        @Param("currency") Currency currency,
                        @Param("status") TransactionStatus status);
}
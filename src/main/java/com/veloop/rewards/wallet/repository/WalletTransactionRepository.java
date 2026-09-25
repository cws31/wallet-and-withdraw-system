package com.veloop.rewards.wallet.repository;

import com.veloop.rewards.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletTransactionRepository
                extends JpaRepository<WalletTransaction, Long> {

        Optional<WalletTransaction> findByTransactionId(String transactionId);

        boolean existsByTransactionId(String transactionId);

        Optional<WalletTransaction> findByReferenceId(String referenceId);

        Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(
                        Long userId,
                        Pageable pageable);

        long countByUserId(Long userId);
}
package com.veloop.rewards.withdrawal.repository;

import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WithdrawalRepository
                extends JpaRepository<Withdrawal, Long> {

        Optional<Withdrawal> findByWithdrawalId(
                        String withdrawalId);

        Page<Withdrawal> findByUserIdOrderByCreatedAtDesc(
                        Long userId,
                        Pageable pageable);

        Page<Withdrawal> findByUserIdAndStatusOrderByCreatedAtDesc(
                        Long userId,
                        WithdrawalStatus status,
                        Pageable pageable);

        boolean existsByWithdrawalId(
                        String withdrawalId);

        long countByUserIdAndCreatedAtAfter(
                        Long userId,
                        LocalDateTime createdAt);

        long countByUserIdAndStatusAndCreatedAtAfter(
                        Long userId,
                        WithdrawalStatus status,
                        LocalDateTime createdAt);

        long countByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        Long userId,
                        Long payoutOptionId,
                        LocalDateTime createdAt);

        boolean existsByUserIdAndPayoutOptionIdAndCreatedAtAfter(
                        Long userId,
                        Long payoutOptionId,
                        LocalDateTime createdAt);

        @Query("""
                        SELECT COUNT(DISTINCT w.payoutOption.id)
                        FROM Withdrawal w
                        WHERE w.user.id = :userId
                          AND w.createdAt > :createdAt
                        """)
        long countDistinctPayoutOptionsByUserIdAndCreatedAtAfter(
                        @Param("userId") Long userId,
                        @Param("createdAt") LocalDateTime createdAt);
}
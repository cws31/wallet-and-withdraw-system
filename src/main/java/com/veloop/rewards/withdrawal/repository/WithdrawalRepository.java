package com.veloop.rewards.withdrawal.repository;

import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithdrawalRepository
        extends JpaRepository<Withdrawal, Long> {

    Optional<Withdrawal> findByWithdrawalId(String withdrawalId);

    Page<Withdrawal> findByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable);

    Page<Withdrawal> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            WithdrawalStatus status,
            Pageable pageable);

    boolean existsByWithdrawalId(String withdrawalId);
}
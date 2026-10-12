package com.veloop.rewards.idempotency.repository;

import com.veloop.rewards.idempotency.entity.WithdrawalIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithdrawalIdempotencyRepository
        extends JpaRepository<WithdrawalIdempotency, Long> {

    Optional<WithdrawalIdempotency> findByUserIdAndIdempotencyKey(
            Long userId,
            String idempotencyKey);

    boolean existsByUserIdAndIdempotencyKey(
            Long userId,
            String idempotencyKey);
}
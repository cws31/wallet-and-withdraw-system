
package com.veloop.rewards.payoutprocessing.repository;

import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutJobRepository
                extends JpaRepository<PayoutJob, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Override
        Optional<PayoutJob> findById(Long id);

        Optional<PayoutJob> findByWithdrawalId(
                        Long withdrawalId);

        boolean existsByWithdrawalId(
                        Long withdrawalId);

        List<PayoutJob> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        PayoutJobStatus status,
                        LocalDateTime nextAttemptAt,
                        Pageable pageable);

        Optional<PayoutJob> findByIdAndStatus(
                        Long id,
                        PayoutJobStatus status);
}
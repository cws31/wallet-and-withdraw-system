package com.veloop.rewards.audit.repository;

import com.veloop.rewards.audit.entity.WithdrawalAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalAuditRepository
        extends JpaRepository<WithdrawalAudit, Long> {

    List<WithdrawalAudit> findByWithdrawalIdOrderByCreatedAtAsc(
            Long withdrawalId);

    List<WithdrawalAudit> findByUserIdOrderByCreatedAtDesc(
            Long userId);
}
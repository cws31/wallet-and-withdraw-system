package com.veloop.rewards.audit.service;

import com.veloop.rewards.audit.entity.WithdrawalAudit;
import com.veloop.rewards.audit.repository.WithdrawalAuditRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WithdrawalAuditService {

    private final WithdrawalAuditRepository withdrawalAuditRepository;

    public WithdrawalAuditService(
            WithdrawalAuditRepository withdrawalAuditRepository) {
        this.withdrawalAuditRepository = withdrawalAuditRepository;
    }

    @Transactional
    public WithdrawalAudit record(
            Withdrawal withdrawal,
            String action,
            WithdrawalStatus oldStatus,
            WithdrawalStatus newStatus,
            User performedBy,
            String description) {

        WithdrawalAudit audit = new WithdrawalAudit();

        audit.setWithdrawal(withdrawal);
        audit.setUser(withdrawal.getUser());
        audit.setAction(action);

        audit.setOldStatus(
                oldStatus == null
                        ? null
                        : oldStatus.name());

        audit.setNewStatus(
                newStatus == null
                        ? null
                        : newStatus.name());

        audit.setPerformedBy(performedBy);
        audit.setDescription(description);

        return withdrawalAuditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalAudit> getWithdrawalHistory(
            Long withdrawalId) {

        return withdrawalAuditRepository
                .findByWithdrawalIdOrderByCreatedAtAsc(withdrawalId);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalAudit> getUserAuditHistory(
            Long userId) {

        return withdrawalAuditRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }
}
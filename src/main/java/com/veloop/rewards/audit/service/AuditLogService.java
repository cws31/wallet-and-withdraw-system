package com.veloop.rewards.audit.service;

import com.veloop.rewards.audit.entity.AuditLog;
import com.veloop.rewards.audit.repository.AuditLogRepository;
import com.veloop.rewards.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog record(
            User actor,
            User targetUser,
            String targetType,
            String action,
            String referenceId,
            String metadata) {

        AuditLog auditLog = new AuditLog();

        auditLog.setActor(actor);
        auditLog.setTargetUser(targetUser);
        auditLog.setTargetType(targetType);
        auditLog.setAction(action);
        auditLog.setReferenceId(referenceId);
        auditLog.setMetadata(metadata);

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getUserAuditHistory(Long userId) {
        return auditLogRepository
                .findByTargetUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getActorAuditHistory(Long actorId) {
        return auditLogRepository
                .findByActorIdOrderByCreatedAtDesc(actorId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getReferenceHistory(
            String targetType,
            String referenceId) {

        return auditLogRepository
                .findByTargetTypeAndReferenceIdOrderByCreatedAtDesc(
                        targetType,
                        referenceId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getActionHistory(String action) {
        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(action);
    }
}
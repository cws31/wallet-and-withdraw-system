package com.veloop.rewards.audit.repository;

import com.veloop.rewards.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    List<AuditLog> findByTargetTypeAndReferenceIdOrderByCreatedAtDesc(
            String targetType,
            String referenceId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}
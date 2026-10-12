package com.veloop.rewards.payoutprocessing.entity;

import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payout_jobs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payout_jobs_withdrawal_id", columnNames = "withdrawal_id")
}, indexes = {
        @Index(name = "idx_payout_jobs_status", columnList = "status"),
        @Index(name = "idx_payout_jobs_next_attempt_at", columnList = "next_attempt_at"),
        @Index(name = "idx_payout_jobs_status_next_attempt", columnList = "status,next_attempt_at")
})
public class PayoutJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "withdrawal_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payout_jobs_withdrawal"))
    private Withdrawal withdrawal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayoutJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (status == null) {
            status = PayoutJobStatus.QUEUED;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Withdrawal getWithdrawal() {
        return withdrawal;
    }

    public PayoutJobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setWithdrawal(
            Withdrawal withdrawal) {

        this.withdrawal = withdrawal;
    }

    public void setStatus(
            PayoutJobStatus status) {

        this.status = status;
    }

    public void setAttemptCount(
            int attemptCount) {

        this.attemptCount = attemptCount;
    }

    public void setNextAttemptAt(
            LocalDateTime nextAttemptAt) {

        this.nextAttemptAt = nextAttemptAt;
    }

    public void setLastError(
            String lastError) {

        this.lastError = lastError;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}

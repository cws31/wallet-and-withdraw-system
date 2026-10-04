package com.veloop.rewards.fraud.entity;

import com.veloop.rewards.fraud.enums.FraudReviewStatus;
import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_risk_events", indexes = {
        @Index(name = "idx_fraud_risk_user", columnList = "user_id"),
        @Index(name = "idx_fraud_risk_withdrawal", columnList = "withdrawal_id"),
        @Index(name = "idx_fraud_risk_decision", columnList = "decision"),
        @Index(name = "idx_fraud_risk_review_status", columnList = "review_status"),
        @Index(name = "idx_fraud_risk_created_at", columnList = "created_at")
})
public class FraudRiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_id")
    private Withdrawal withdrawal;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private FraudRiskDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private FraudReviewStatus reviewStatus;

    @Column(name = "triggered_rules", nullable = false, columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (reviewStatus == null) {
            reviewStatus = FraudReviewStatus.NOT_REQUIRED;
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Withdrawal getWithdrawal() {
        return withdrawal;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public FraudRiskDecision getDecision() {
        return decision;
    }

    public FraudReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public String getTriggeredRules() {
        return triggeredRules;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setWithdrawal(Withdrawal withdrawal) {
        this.withdrawal = withdrawal;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public void setDecision(
            FraudRiskDecision decision) {

        this.decision = decision;
    }

    public void setReviewStatus(
            FraudReviewStatus reviewStatus) {

        this.reviewStatus = reviewStatus;
    }

    public void setTriggeredRules(
            String triggeredRules) {

        this.triggeredRules = triggeredRules;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public void setReviewedAt(
            LocalDateTime reviewedAt) {

        this.reviewedAt = reviewedAt;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }
}
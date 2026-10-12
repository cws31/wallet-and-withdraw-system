package com.veloop.rewards.fraud.entity;

import com.veloop.rewards.fraud.enums.FraudReviewStatus;
import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_risk_events", indexes = {
        @Index(name = "idx_fraud_risk_events_user_id", columnList = "user_id"),
        @Index(name = "idx_fraud_risk_events_withdrawal_id", columnList = "withdrawal_id"),
        @Index(name = "idx_fraud_risk_events_decision", columnList = "decision"),
        @Index(name = "idx_fraud_risk_events_created_at", columnList = "created_at")
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
    @Column(name = "review_status", nullable = false, length = 20)
    private FraudReviewStatus reviewStatus;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Withdrawal getWithdrawal() {
        return withdrawal;
    }

    public void setWithdrawal(Withdrawal withdrawal) {
        this.withdrawal = withdrawal;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public FraudRiskDecision getDecision() {
        return decision;
    }

    public void setDecision(FraudRiskDecision decision) {
        this.decision = decision;
    }

    public FraudReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(FraudReviewStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(String triggeredRules) {
        this.triggeredRules = triggeredRules;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.veloop.rewards.withdrawal.entity;

import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_withdrawals_withdrawal_id", columnNames = "withdrawal_id")
}, indexes = {
        @Index(name = "idx_withdrawals_user_id", columnList = "user_id"),
        @Index(name = "idx_withdrawals_status", columnList = "status"),
        @Index(name = "idx_withdrawals_created_at", columnList = "created_at"),
        @Index(name = "idx_withdrawals_user_status", columnList = "user_id,status"),
        @Index(name = "idx_withdrawals_payout_option_id", columnList = "payout_option_id")
})
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdrawal_id", nullable = false, unique = true, length = 100)
    private String withdrawalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_withdrawals_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payout_method_id", nullable = false, foreignKey = @ForeignKey(name = "fk_withdrawals_payout_method"))
    private PayoutMethod payoutMethod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payout_option_id", nullable = false, foreignKey = @ForeignKey(name = "fk_withdrawals_payout_option"))
    private PayoutOption payoutOption;

    @Column(name = "currency", nullable = false, length = 20)
    private String currency;

    @Column(name = "currency_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currencyAmount;

    @Column(name = "payout_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal payoutAmount;

    @Column(name = "payout_details", columnDefinition = "TEXT")
    private String payoutDetails;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", foreignKey = @ForeignKey(name = "fk_withdrawals_transaction"))
    private com.veloop.rewards.wallet.entity.WalletTransaction transaction;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (requestedAt == null) {
            requestedAt = now;
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

    public String getWithdrawalId() {
        return withdrawalId;
    }

    public User getUser() {
        return user;
    }

    public PayoutMethod getPayoutMethod() {
        return payoutMethod;
    }

    public PayoutOption getPayoutOption() {
        return payoutOption;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public String getPayoutDetails() {
        return payoutDetails;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public com.veloop.rewards.wallet.entity.WalletTransaction getTransaction() {
        return transaction;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
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

    public void setWithdrawalId(String withdrawalId) {
        this.withdrawalId = withdrawalId;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPayoutMethod(PayoutMethod payoutMethod) {
        this.payoutMethod = payoutMethod;
    }

    public void setPayoutOption(PayoutOption payoutOption) {
        this.payoutOption = payoutOption;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public void setPayoutAmount(BigDecimal payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public void setPayoutDetails(String payoutDetails) {
        this.payoutDetails = payoutDetails;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public void setTransaction(
            com.veloop.rewards.wallet.entity.WalletTransaction transaction) {
        this.transaction = transaction;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
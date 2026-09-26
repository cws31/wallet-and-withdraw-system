package com.veloop.rewards.withdrawal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WithdrawalResponse {

    private String withdrawalId;
    private Long payoutMethodId;
    private String payoutMethod;
    private Long payoutOptionId;
    private String currency;
    private BigDecimal currencyAmount;
    private BigDecimal payoutAmount;
    private String payoutDetails;
    private String status;
    private String rejectionReason;
    private String reviewNote;
    private Long transactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getWithdrawalId() {
        return withdrawalId;
    }

    public void setWithdrawalId(String withdrawalId) {
        this.withdrawalId = withdrawalId;
    }

    public Long getPayoutMethodId() {
        return payoutMethodId;
    }

    public void setPayoutMethodId(Long payoutMethodId) {
        this.payoutMethodId = payoutMethodId;
    }

    public String getPayoutMethod() {
        return payoutMethod;
    }

    public void setPayoutMethod(String payoutMethod) {
        this.payoutMethod = payoutMethod;
    }

    public Long getPayoutOptionId() {
        return payoutOptionId;
    }

    public void setPayoutOptionId(Long payoutOptionId) {
        this.payoutOptionId = payoutOptionId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(BigDecimal payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public String getPayoutDetails() {
        return payoutDetails;
    }

    public void setPayoutDetails(String payoutDetails) {
        this.payoutDetails = payoutDetails;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
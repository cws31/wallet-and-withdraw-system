package com.veloop.rewards.payout.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_options")
public class PayoutOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payout_option_method"))
    private PayoutMethod method;

    @Column(name = "payout_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal payoutAmount;

    @Column(nullable = false, length = 20)
    private String currency = "INR";

    @Column(name = "currency_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currencyAmount;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

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

    public PayoutMethod getMethod() {
        return method;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public Boolean getActive() {
        return active;
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

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public void setPayoutAmount(BigDecimal payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

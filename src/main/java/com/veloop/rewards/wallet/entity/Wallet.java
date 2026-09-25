package com.veloop.rewards.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "ves", nullable = false, precision = 19, scale = 4)
    private BigDecimal ves = BigDecimal.ZERO;

    @Column(name = "sves", nullable = false, precision = 19, scale = 4)
    private BigDecimal sves = BigDecimal.ZERO;

    @Column(name = "gems", nullable = false, precision = 19, scale = 4)
    private BigDecimal gems = BigDecimal.ZERO;

    @Column(name = "tokens", nullable = false, precision = 19, scale = 4)
    private BigDecimal tokens = BigDecimal.ZERO;

    @Column(name = "spins", nullable = false, precision = 19, scale = 4)
    private BigDecimal spins = BigDecimal.ZERO;

    @Column(name = "withdrawn_ves", nullable = false, precision = 19, scale = 4)
    private BigDecimal withdrawnVes = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getVes() {
        return ves;
    }

    public BigDecimal getSves() {
        return sves;
    }

    public BigDecimal getGems() {
        return gems;
    }

    public BigDecimal getTokens() {
        return tokens;
    }

    public BigDecimal getSpins() {
        return spins;
    }

    public BigDecimal getWithdrawnVes() {
        return withdrawnVes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setVes(BigDecimal ves) {
        this.ves = ves;
    }

    public void setSves(BigDecimal sves) {
        this.sves = sves;
    }

    public void setGems(BigDecimal gems) {
        this.gems = gems;
    }

    public void setTokens(BigDecimal tokens) {
        this.tokens = tokens;
    }

    public void setSpins(BigDecimal spins) {
        this.spins = spins;
    }

    public void setWithdrawnVes(BigDecimal withdrawnVes) {
        this.withdrawnVes = withdrawnVes;
    }
}
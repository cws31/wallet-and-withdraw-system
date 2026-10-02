package com.veloop.rewards.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private int loginRequests = 5;
    private int loginWindowSeconds = 60;

    private int withdrawalRequests = 5;
    private int withdrawalWindowSeconds = 60;

    private int walletMutationRequests = 20;
    private int walletMutationWindowSeconds = 60;

    private int withdrawalMutationRequests = 20;
    private int withdrawalMutationWindowSeconds = 60;

    private int maxEntries = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLoginRequests() {
        return loginRequests;
    }

    public void setLoginRequests(int loginRequests) {
        this.loginRequests = loginRequests;
    }

    public int getLoginWindowSeconds() {
        return loginWindowSeconds;
    }

    public void setLoginWindowSeconds(int loginWindowSeconds) {
        this.loginWindowSeconds = loginWindowSeconds;
    }

    public int getWithdrawalRequests() {
        return withdrawalRequests;
    }

    public void setWithdrawalRequests(int withdrawalRequests) {
        this.withdrawalRequests = withdrawalRequests;
    }

    public int getWithdrawalWindowSeconds() {
        return withdrawalWindowSeconds;
    }

    public void setWithdrawalWindowSeconds(int withdrawalWindowSeconds) {
        this.withdrawalWindowSeconds = withdrawalWindowSeconds;
    }

    public int getWalletMutationRequests() {
        return walletMutationRequests;
    }

    public void setWalletMutationRequests(int walletMutationRequests) {
        this.walletMutationRequests = walletMutationRequests;
    }

    public int getWalletMutationWindowSeconds() {
        return walletMutationWindowSeconds;
    }

    public void setWalletMutationWindowSeconds(int walletMutationWindowSeconds) {
        this.walletMutationWindowSeconds = walletMutationWindowSeconds;
    }

    public int getWithdrawalMutationRequests() {
        return withdrawalMutationRequests;
    }

    public void setWithdrawalMutationRequests(int withdrawalMutationRequests) {
        this.withdrawalMutationRequests = withdrawalMutationRequests;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}
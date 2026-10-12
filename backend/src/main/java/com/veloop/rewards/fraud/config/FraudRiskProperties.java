package com.veloop.rewards.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fraud.risk")
public class FraudRiskProperties {

    private int reviewThreshold = 30;
    private int blockThreshold = 60;
    private int maximumScore = 100;

    public int getReviewThreshold() {
        return reviewThreshold;
    }

    public void setReviewThreshold(int reviewThreshold) {
        this.reviewThreshold = reviewThreshold;
    }

    public int getBlockThreshold() {
        return blockThreshold;
    }

    public void setBlockThreshold(int blockThreshold) {
        this.blockThreshold = blockThreshold;
    }

    public int getMaximumScore() {
        return maximumScore;
    }

    public void setMaximumScore(int maximumScore) {
        this.maximumScore = maximumScore;
    }
}

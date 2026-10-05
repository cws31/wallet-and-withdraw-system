package com.veloop.rewards.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fraud.rule")
public class FraudRuleProperties {

    private RapidWithdrawal rapidWithdrawal = new RapidWithdrawal();

    public RapidWithdrawal getRapidWithdrawal() {
        return rapidWithdrawal;
    }

    public void setRapidWithdrawal(RapidWithdrawal rapidWithdrawal) {
        this.rapidWithdrawal = rapidWithdrawal;
    }

    public static class RapidWithdrawal {

        private long windowMinutes = 5;
        private int baseline = 1;
        private int maxExpected = 5;
        private int maxScore = 40;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(int maxScore) {
            this.maxScore = maxScore;
        }
    }
}

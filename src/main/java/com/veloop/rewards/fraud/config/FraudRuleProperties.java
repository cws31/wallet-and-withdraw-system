package com.veloop.rewards.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fraud.rule")
public class FraudRuleProperties {

    private RapidWithdrawal rapidWithdrawal = new RapidWithdrawal();

    private RepeatedWithdrawal repeatedWithdrawal = new RepeatedWithdrawal();

    public RapidWithdrawal getRapidWithdrawal() {
        return rapidWithdrawal;
    }

    public void setRapidWithdrawal(
            RapidWithdrawal rapidWithdrawal) {
        this.rapidWithdrawal = rapidWithdrawal;
    }

    public RepeatedWithdrawal getRepeatedWithdrawal() {
        return repeatedWithdrawal;
    }

    public void setRepeatedWithdrawal(
            RepeatedWithdrawal repeatedWithdrawal) {
        this.repeatedWithdrawal = repeatedWithdrawal;
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

    public static class RepeatedWithdrawal {

        private long windowHours = 24;
        private int baseline = 2;
        private int maxExpected = 8;
        private int maxScore = 40;

        public long getWindowHours() {
            return windowHours;
        }

        public void setWindowHours(long windowHours) {
            this.windowHours = windowHours;
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
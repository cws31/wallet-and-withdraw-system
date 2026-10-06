package com.veloop.rewards.fraud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fraud.rule")
public class FraudRuleProperties {

    private RapidWithdrawal rapidWithdrawal = new RapidWithdrawal();

    private RepeatedWithdrawal repeatedWithdrawal = new RepeatedWithdrawal();

    private FailedRequests failedRequests = new FailedRequests();

    private UnusualWalletActivity unusualWalletActivity = new UnusualWalletActivity();

    private SuspiciousPayout suspiciousPayout = new SuspiciousPayout();

    private SuspiciousAccountActivity suspiciousAccountActivity = new SuspiciousAccountActivity();

    private MultipleSuspiciousPayoutPattern multipleSuspiciousPayoutPattern = new MultipleSuspiciousPayoutPattern();

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

    public FailedRequests getFailedRequests() {
        return failedRequests;
    }

    public void setFailedRequests(
            FailedRequests failedRequests) {
        this.failedRequests = failedRequests;
    }

    public UnusualWalletActivity getUnusualWalletActivity() {
        return unusualWalletActivity;
    }

    public void setUnusualWalletActivity(
            UnusualWalletActivity unusualWalletActivity) {
        this.unusualWalletActivity = unusualWalletActivity;
    }

    public SuspiciousPayout getSuspiciousPayout() {
        return suspiciousPayout;
    }

    public void setSuspiciousPayout(
            SuspiciousPayout suspiciousPayout) {
        this.suspiciousPayout = suspiciousPayout;
    }

    public SuspiciousAccountActivity getSuspiciousAccountActivity() {
        return suspiciousAccountActivity;
    }

    public void setSuspiciousAccountActivity(
            SuspiciousAccountActivity suspiciousAccountActivity) {
        this.suspiciousAccountActivity = suspiciousAccountActivity;
    }

    public MultipleSuspiciousPayoutPattern getMultipleSuspiciousPayoutPattern() {
        return multipleSuspiciousPayoutPattern;
    }

    public void setMultipleSuspiciousPayoutPattern(
            MultipleSuspiciousPayoutPattern multipleSuspiciousPayoutPattern) {
        this.multipleSuspiciousPayoutPattern = multipleSuspiciousPayoutPattern;
    }

    public static class RapidWithdrawal {

        private long windowMinutes = 5;
        private int baseline = 1;
        private int maxExpected = 5;
        private int maxScore = 40;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(
                long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(
                int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(
                int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(
                int maxScore) {
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

        public void setWindowHours(
                long windowHours) {
            this.windowHours = windowHours;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(
                int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(
                int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(
                int maxScore) {
            this.maxScore = maxScore;
        }
    }

    public static class FailedRequests {

        private long windowMinutes = 10;
        private int baseline = 2;
        private int maxExpected = 8;
        private int maxScore = 35;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(
                long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(
                int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(
                int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(
                int maxScore) {
            this.maxScore = maxScore;
        }
    }

    public static class UnusualWalletActivity {

        private long windowMinutes = 15;
        private int baseline = 4;
        private int maxExpected = 16;
        private int maxScore = 35;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(
                long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(
                int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(
                int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(
                int maxScore) {
            this.maxScore = maxScore;
        }
    }

    public static class SuspiciousPayout {

        private long windowMinutes = 30;
        private int baseline = 1;
        private int maxExpected = 7;
        private int maxScore = 45;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(
                long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(
                int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(
                int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(
                int maxScore) {
            this.maxScore = maxScore;
        }
    }

    public static class SuspiciousAccountActivity {

        private long windowMinutes = 10;
        private int baseline = 2;
        private int maxExpected = 8;
        private int maxScore = 40;

        public long getWindowMinutes() {
            return windowMinutes;
        }

        public void setWindowMinutes(
                long windowMinutes) {
            this.windowMinutes = windowMinutes;
        }

        public int getBaseline() {
            return baseline;
        }

        public void setBaseline(
                int baseline) {
            this.baseline = baseline;
        }

        public int getMaxExpected() {
            return maxExpected;
        }

        public void setMaxExpected(
                int maxExpected) {
            this.maxExpected = maxExpected;
        }

        public int getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(
                int maxScore) {
            this.maxScore = maxScore;
        }
    }

    public static class MultipleSuspiciousPayoutPattern {

        private long windowMinutes = 30;

        private int baseline = 1;

        private int maxExpected = 4;

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

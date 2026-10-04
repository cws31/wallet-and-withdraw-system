package com.veloop.rewards.fraud.repository;

import com.veloop.rewards.fraud.entity.FraudRiskEvent;
import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import com.veloop.rewards.fraud.enums.FraudReviewStatus;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudRiskEventRepository
        extends JpaRepository<FraudRiskEvent, Long> {

    List<FraudRiskEvent> findByUserOrderByCreatedAtDesc(
            User user);

    List<FraudRiskEvent> findByWithdrawalOrderByCreatedAtDesc(
            Withdrawal withdrawal);

    List<FraudRiskEvent> findByDecisionOrderByCreatedAtDesc(
            FraudRiskDecision decision);

    List<FraudRiskEvent> findByReviewStatusOrderByCreatedAtDesc(
            FraudReviewStatus reviewStatus);

    long countByUserAndCreatedAtAfter(
            User user,
            LocalDateTime createdAt);

    long countByUserAndDecisionAndCreatedAtAfter(
            User user,
            FraudRiskDecision decision,
            LocalDateTime createdAt);

    long countByUserAndReviewStatusAndCreatedAtAfter(
            User user,
            FraudReviewStatus reviewStatus,
            LocalDateTime createdAt);
}
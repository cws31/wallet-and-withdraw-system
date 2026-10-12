package com.veloop.rewards.fraud.service;

import com.veloop.rewards.fraud.config.FraudRiskProperties;
import com.veloop.rewards.fraud.entity.FraudRiskEvent;
import com.veloop.rewards.fraud.enums.FraudReviewStatus;
import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.fraud.exception.FraudRiskBlockedException;
import com.veloop.rewards.fraud.repository.FraudRiskEventRepository;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FraudRiskService {

        private final List<FraudRiskRule> rules;
        private final FraudRiskProperties properties;
        private final FraudRiskEventRepository eventRepository;
        private final UserRepository userRepository;

        public FraudRiskService(
                        List<FraudRiskRule> rules,
                        FraudRiskProperties properties,
                        FraudRiskEventRepository eventRepository,
                        UserRepository userRepository) {

                this.rules = rules;
                this.properties = properties;
                this.eventRepository = eventRepository;
                this.userRepository = userRepository;
        }

        public FraudRiskEvaluation evaluate(
                        Long userId,
                        LocalDateTime evaluationTime) {

                List<RiskRuleResult> results = rules.stream()
                                .map(rule -> rule.evaluate(
                                                userId,
                                                evaluationTime))
                                .toList();

                return buildEvaluation(results);
        }

        public FraudRiskEvaluation evaluate(
                        Long userId,
                        LocalDateTime evaluationTime,
                        Long payoutOptionId) {

                List<RiskRuleResult> results = rules.stream()
                                .map(rule -> rule.evaluate(
                                                userId,
                                                evaluationTime,
                                                payoutOptionId))
                                .toList();

                return buildEvaluation(results);
        }

        private FraudRiskEvaluation buildEvaluation(
                        List<RiskRuleResult> results) {

                int totalRiskScore = Math.min(
                                properties.getMaximumScore(),
                                results.stream()
                                                .mapToInt(
                                                                RiskRuleResult::riskScore)
                                                .sum());

                FraudRiskDecision decision = determineDecision(totalRiskScore);

                return new FraudRiskEvaluation(
                                totalRiskScore,
                                decision,
                                results);
        }

        @Transactional
        public FraudRiskEvent saveEvent(
                        Long userId,
                        Withdrawal withdrawal,
                        FraudRiskEvaluation evaluation) {

                return saveEventInternal(
                                userId,
                                withdrawal,
                                evaluation);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public FraudRiskEvent saveBlockedEvent(
                        Long userId,
                        FraudRiskEvaluation evaluation) {

                return saveEventInternal(
                                userId,
                                null,
                                evaluation);
        }

        private FraudRiskEvent saveEventInternal(
                        Long userId,
                        Withdrawal withdrawal,
                        FraudRiskEvaluation evaluation) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "User not found while recording fraud risk event"));

                FraudRiskEvent event = new FraudRiskEvent();

                event.setUser(user);
                event.setWithdrawal(withdrawal);
                event.setRiskScore(
                                evaluation.totalRiskScore());
                event.setDecision(
                                evaluation.decision());
                event.setReviewStatus(
                                reviewStatusFor(
                                                evaluation.decision()));

                event.setTriggeredRules(
                                evaluation.triggeredRules()
                                                .stream()
                                                .map(result -> result.ruleCode()
                                                                + "="
                                                                + result.riskScore())
                                                .collect(
                                                                Collectors.joining(",")));

                event.setExplanation(
                                evaluation.ruleResults()
                                                .stream()
                                                .map(RiskRuleResult::explanation)
                                                .collect(
                                                                Collectors.joining(" | ")));

                return eventRepository.save(event);
        }

        private FraudRiskDecision determineDecision(
                        int totalRiskScore) {

                if (totalRiskScore >= properties.getBlockThreshold()) {

                        return FraudRiskDecision.BLOCK;
                }

                if (totalRiskScore >= properties.getReviewThreshold()) {

                        return FraudRiskDecision.REVIEW;
                }

                return FraudRiskDecision.ALLOW;
        }

        private FraudReviewStatus reviewStatusFor(
                        FraudRiskDecision decision) {

                return decision == FraudRiskDecision.REVIEW
                                ? FraudReviewStatus.PENDING
                                : FraudReviewStatus.NOT_REQUIRED;
        }
}
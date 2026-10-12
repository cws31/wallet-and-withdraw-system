package com.veloop.rewards.auth.repository;

import com.veloop.rewards.auth.entity.AuthenticationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuthenticationAttemptRepository
        extends JpaRepository<AuthenticationAttempt, Long> {

    long countByUserIdAndSuccessFalseAndCreatedAtAfter(
            Long userId,
            LocalDateTime createdAt);

    long countByUserIdAndSuccessTrueAndCreatedAtAfter(
            Long userId,
            LocalDateTime createdAt);

    long countByEmailAndSuccessFalseAndCreatedAtAfter(
            String email,
            LocalDateTime createdAt);
}
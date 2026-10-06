package com.veloop.rewards.auth.entity;

import com.veloop.rewards.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "authentication_attempts", indexes = {
        @Index(name = "idx_authentication_attempts_user_id", columnList = "user_id"),
        @Index(name = "idx_authentication_attempts_email", columnList = "email"),
        @Index(name = "idx_authentication_attempts_success_created_at", columnList = "success, created_at"),
        @Index(name = "idx_authentication_attempts_user_success_created_at", columnList = "user_id, success, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_authentication_attempts_user"))
    private User user;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
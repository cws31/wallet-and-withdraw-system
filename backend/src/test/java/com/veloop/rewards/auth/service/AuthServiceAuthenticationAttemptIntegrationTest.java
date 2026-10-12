package com.veloop.rewards.auth.service;

import com.veloop.rewards.auth.dto.LoginRequest;
import com.veloop.rewards.auth.entity.AuthenticationAttempt;
import com.veloop.rewards.auth.repository.AuthenticationAttemptRepository;
import com.veloop.rewards.common.exception.AuthenticationFailedException;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.service.WalletService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceAuthenticationAttemptIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationAttemptRepository authenticationAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WalletService walletService;

    private User user;

    private String email;

    @BeforeEach
    void setUp() {

        email = "auth-attempt-"
                + UUID.randomUUID()
                + "@example.com";

        user = User.builder()
                .name("Authentication Attempt Test User")
                .email(email)
                .passwordHash(
                        passwordEncoder.encode("Password@123"))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(true)
                .level(0)
                .build();

        user = userRepository.saveAndFlush(user);

        authenticationAttemptRepository.deleteAll();
    }

    @Test
    void wrongPasswordShouldPersistFailedAuthenticationAttempt() {

        LoginRequest request = new LoginRequest(
                email,
                "WrongPassword@123");

        assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login(request));

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();

        assertEquals(1, attempts.size());

        AuthenticationAttempt attempt = attempts.get(0);

        assertEquals(user.getId(), attempt.getUser().getId());
        assertEquals(email, attempt.getEmail());
        assertFalse(attempt.getSuccess());
        assertNotNull(attempt.getCreatedAt());
    }

    @Test
    void unknownEmailShouldPersistFailedAuthenticationAttemptWithoutUser() {

        String unknownEmail = "unknown-"
                + UUID.randomUUID()
                + "@example.com";

        LoginRequest request = new LoginRequest(
                unknownEmail,
                "Password@123");

        assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login(request));

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();

        assertEquals(1, attempts.size());

        AuthenticationAttempt attempt = attempts.get(0);

        assertNull(attempt.getUser());
        assertEquals(unknownEmail, attempt.getEmail());
        assertFalse(attempt.getSuccess());
        assertNotNull(attempt.getCreatedAt());
    }

    @Test
    void correctPasswordShouldPersistSuccessfulAuthenticationAttempt() {

        LoginRequest request = new LoginRequest(
                email,
                "Password@123");

        var response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.token());

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();

        assertEquals(1, attempts.size());

        AuthenticationAttempt attempt = attempts.get(0);

        assertEquals(user.getId(), attempt.getUser().getId());
        assertEquals(email, attempt.getEmail());
        assertTrue(attempt.getSuccess());
        assertNotNull(attempt.getCreatedAt());
    }

    @Test
    void inactiveAccountShouldPersistFailedAuthenticationAttempt() {

        user.setAccountStatus("INACTIVE");
        userRepository.saveAndFlush(user);

        LoginRequest request = new LoginRequest(
                email,
                "Password@123");

        assertThrows(
                AuthenticationFailedException.class,
                () -> authService.login(request));

        List<AuthenticationAttempt> attempts = authenticationAttemptRepository.findAll();

        assertEquals(1, attempts.size());

        AuthenticationAttempt attempt = attempts.get(0);

        assertEquals(user.getId(), attempt.getUser().getId());
        assertEquals(email, attempt.getEmail());
        assertFalse(attempt.getSuccess());
        assertNotNull(attempt.getCreatedAt());
    }
}

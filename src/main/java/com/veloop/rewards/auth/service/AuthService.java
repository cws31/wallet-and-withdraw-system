package com.veloop.rewards.auth.service;

import com.veloop.rewards.auth.dto.LoginRequest;
import com.veloop.rewards.auth.dto.LoginResponse;
import com.veloop.rewards.auth.dto.RegisterRequest;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role("USER")
                .accountStatus("ACTIVE")
                .verified(false)
                .level(0)
                .build();

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!"ACTIVE".equals(user.getAccountStatus())) {
            throw new IllegalArgumentException("User account is not active");
        }

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole());
    }
}
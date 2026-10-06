package com.veloop.rewards.auth.service;

import com.veloop.rewards.auth.dto.LoginRequest;
import com.veloop.rewards.auth.dto.LoginResponse;
import com.veloop.rewards.auth.dto.RegisterRequest;
import com.veloop.rewards.auth.entity.AuthenticationAttempt;
import com.veloop.rewards.auth.repository.AuthenticationAttemptRepository;
import com.veloop.rewards.common.exception.AuthenticationFailedException;
import com.veloop.rewards.common.exception.BusinessException;
import com.veloop.rewards.security.JwtService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.service.WalletService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final WalletService walletService;
        private final AuthenticationAttemptRepository authenticationAttemptRepository;

        public AuthService(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        WalletService walletService,
                        AuthenticationAttemptRepository authenticationAttemptRepository) {

                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtService = jwtService;
                this.walletService = walletService;
                this.authenticationAttemptRepository = authenticationAttemptRepository;
        }

        @Transactional
        public User register(RegisterRequest request) {

                String email = request.email().trim().toLowerCase();

                if (userRepository.existsByEmail(email)) {
                        throw new BusinessException(
                                        "Email is already registered");
                }

                User user = User.builder()
                                .name(request.name().trim())
                                .email(email)
                                .passwordHash(
                                                passwordEncoder.encode(
                                                                request.password()))
                                .role("USER")
                                .accountStatus("ACTIVE")
                                .verified(false)
                                .level(0)
                                .build();

                User savedUser = userRepository.save(user);

                walletService.createWallet(savedUser.getId());

                return savedUser;
        }

        public LoginResponse login(LoginRequest request) {

                String email = request.email().trim().toLowerCase();

                User user = userRepository.findByEmail(email)
                                .orElse(null);

                if (user == null) {

                        saveAuthenticationAttempt(
                                        null,
                                        email,
                                        false);

                        throw new AuthenticationFailedException(
                                        "Invalid email or password");
                }

                if (!"ACTIVE".equals(user.getAccountStatus())) {

                        saveAuthenticationAttempt(
                                        user,
                                        email,
                                        false);

                        throw new AuthenticationFailedException(
                                        "User account is not active");
                }

                if (!passwordEncoder.matches(
                                request.password(),
                                user.getPasswordHash())) {

                        saveAuthenticationAttempt(
                                        user,
                                        email,
                                        false);

                        throw new AuthenticationFailedException(
                                        "Invalid email or password");
                }

                saveAuthenticationAttempt(
                                user,
                                email,
                                true);

                String token = jwtService.generateToken(
                                user.getId(),
                                user.getEmail(),
                                user.getRole());

                return new LoginResponse(
                                user.getId(),
                                user.getEmail(),
                                user.getName(),
                                user.getRole(),
                                token);
        }

        private void saveAuthenticationAttempt(
                        User user,
                        String email,
                        boolean success) {

                AuthenticationAttempt attempt = AuthenticationAttempt.builder()
                                .user(user)
                                .email(email)
                                .success(success)
                                .build();

                authenticationAttemptRepository.save(attempt);
        }
}

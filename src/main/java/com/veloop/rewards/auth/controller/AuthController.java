package com.veloop.rewards.auth.controller;

import com.veloop.rewards.auth.dto.CurrentUserResponse;
import com.veloop.rewards.auth.dto.LoginRequest;
import com.veloop.rewards.auth.dto.LoginResponse;
import com.veloop.rewards.auth.dto.RegisterRequest;
import com.veloop.rewards.auth.dto.RegisterResponse;
import com.veloop.rewards.auth.service.AuthService;
import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        RegisterResponse response = new RegisterResponse(user.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User registered successfully",
                                response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        CurrentUserResponse response = new CurrentUserResponse(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Current user retrieved successfully",
                        response));
    }
}
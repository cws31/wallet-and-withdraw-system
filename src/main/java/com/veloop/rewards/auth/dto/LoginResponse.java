package com.veloop.rewards.auth.dto;

public record LoginResponse(
        Long userId,
        String email,
        String name,
        String role,
        String token) {
}
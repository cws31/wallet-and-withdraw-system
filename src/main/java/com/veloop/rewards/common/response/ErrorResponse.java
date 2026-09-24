package com.veloop.rewards.common.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String message,
        LocalDateTime timestamp,
        String path,
        Map<String, String> errors) {

    public static ErrorResponse of(
            String message,
            String path) {
        return new ErrorResponse(
                false,
                message,
                LocalDateTime.now(),
                path,
                null);
    }

    public static ErrorResponse validation(
            String message,
            String path,
            Map<String, String> errors) {
        return new ErrorResponse(
                false,
                message,
                LocalDateTime.now(),
                path,
                errors);
    }
}
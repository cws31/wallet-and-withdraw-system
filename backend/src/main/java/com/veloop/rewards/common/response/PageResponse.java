package com.veloop.rewards.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int limit,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {
}
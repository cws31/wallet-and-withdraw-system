package com.veloop.rewards.withdrawal.controller;

import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.common.response.PageResponse;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdrawals")
@Tag(name = "Withdrawals", description = "Wallet withdrawal request APIs")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(
            WithdrawalService withdrawalService) {

        this.withdrawalService = withdrawalService;
    }

    @PostMapping
    @Operation(summary = "Create withdrawal request", description = "Creates a withdrawal request for the authenticated user. "
            + "Payout amount and required VES are resolved from backend configuration.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WithdrawalResponse>> createWithdrawal(
            Authentication authentication,
            @Valid @RequestBody WithdrawalCreateRequest request) {

        Long userId = Long.valueOf(authentication.getName());

        WithdrawalResponse response = withdrawalService.createWithdrawal(
                userId,
                request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Withdrawal request created successfully",
                                response));
    }

    @GetMapping
    @Operation(summary = "Get withdrawal history", description = "Returns paginated withdrawal history for the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PageResponse<WithdrawalResponse>>> getWithdrawals(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        Long userId = Long.valueOf(authentication.getName());

        int safePage = Math.max(page - 1, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        PageRequest pageable = PageRequest.of(
                safePage,
                safeLimit,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"));

        PageResponse<WithdrawalResponse> withdrawals = withdrawalService.getWithdrawals(
                userId,
                pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Withdrawal history retrieved successfully",
                        withdrawals));
    }

    @GetMapping("/{withdrawalId}")
    @Operation(summary = "Get withdrawal details", description = "Returns a specific withdrawal belonging to the authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WithdrawalResponse>> getWithdrawal(
            Authentication authentication,
            @PathVariable String withdrawalId) {

        Long userId = Long.valueOf(authentication.getName());

        WithdrawalResponse response = withdrawalService.getWithdrawal(
                userId,
                withdrawalId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Withdrawal retrieved successfully",
                        response));
    }
}
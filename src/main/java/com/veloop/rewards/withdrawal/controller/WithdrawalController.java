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
import org.springframework.security.access.prepost.PreAuthorize;

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

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{withdrawalId}/processing")
    @Operation(summary = "Move withdrawal to processing", description = "Marks a pending withdrawal as PROCESSING. Admin only.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WithdrawalResponse>> markProcessing(
            @PathVariable String withdrawalId) {

        WithdrawalResponse response = withdrawalService.markProcessing(withdrawalId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Withdrawal moved to processing",
                        response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{withdrawalId}/approve")
    @Operation(summary = "Approve withdrawal", description = "Approves a pending or processing withdrawal. Admin only.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WithdrawalResponse>> approveWithdrawal(
            @PathVariable String withdrawalId) {

        WithdrawalResponse response = withdrawalService.approveWithdrawal(withdrawalId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Withdrawal approved successfully",
                        response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{withdrawalId}/reject")
    @Operation(summary = "Reject withdrawal", description = "Rejects a withdrawal and reverses the previously deducted VES. Admin only.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WithdrawalResponse>> rejectWithdrawal(
            @PathVariable String withdrawalId,
            @RequestParam String rejectionReason,
            @RequestParam(required = false) String reviewNote) {

        WithdrawalResponse response = withdrawalService.rejectWithdrawal(
                withdrawalId,
                rejectionReason,
                reviewNote);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Withdrawal rejected and balance reversed",
                        response));
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{withdrawalId}/cancel")
    @Operation(summary = "Cancel withdrawal", description = "Cancels a pending withdrawal and reverses the previously deducted VES.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WithdrawalResponse>> cancelWithdrawal(
            Authentication authentication,
            @PathVariable String withdrawalId) {

        Long userId = Long.valueOf(authentication.getName());

        WithdrawalResponse response = withdrawalService.cancelWithdrawal(
                userId,
                withdrawalId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Withdrawal cancelled and balance reversed",
                        response));
    }
}
package com.veloop.rewards.reconciliation.controller;

import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.reconciliation.dto.ReconciliationResult;
import com.veloop.rewards.reconciliation.service.ReconciliationService;
import com.veloop.rewards.wallet.enums.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reconciliation")
@Tag(name = "Financial Reconciliation", description = "Administrative wallet and ledger reconciliation APIs")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService) {

        this.reconciliationService = reconciliationService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/wallet/{walletId}")
    @Operation(summary = "Reconcile wallet balance", description = "Compares the current wallet balance with the balance derived from completed ledger transactions. Admin only.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<ReconciliationResult>> reconcileWallet(
            @PathVariable Long walletId,
            @RequestParam Currency currency) {

        ReconciliationResult result = reconciliationService.reconcile(
                walletId,
                currency);

        String message = result.reconciled()
                ? "Wallet reconciliation completed successfully"
                : "Wallet reconciliation detected a balance mismatch";

        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        result));
    }
}
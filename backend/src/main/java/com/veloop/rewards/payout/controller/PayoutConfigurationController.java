package com.veloop.rewards.payout.controller;

import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.payout.dto.PayoutMethodResponse;
import com.veloop.rewards.payout.service.PayoutConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payouts")
@Tag(name = "Payout Configuration", description = "Payout methods and payout options APIs")
public class PayoutConfigurationController {

    private final PayoutConfigurationService payoutConfigurationService;

    public PayoutConfigurationController(
            PayoutConfigurationService payoutConfigurationService) {

        this.payoutConfigurationService = payoutConfigurationService;
    }

    @GetMapping("/configuration")
    @Operation(summary = "Get payout configuration", description = "Returns all active payout methods and their active payout options.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<PayoutMethodResponse>>> getPayoutConfiguration() {

        List<PayoutMethodResponse> configuration = payoutConfigurationService.getActivePayoutMethods();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payout configuration retrieved successfully",
                        configuration));
    }
}
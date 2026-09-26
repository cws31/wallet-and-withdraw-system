
package com.veloop.rewards.payout.controller;

import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.payout.dto.PayoutMethodResponse;
import com.veloop.rewards.payout.service.PayoutConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payouts")
public class PayoutConfigurationController {

    private final PayoutConfigurationService payoutConfigurationService;

    public PayoutConfigurationController(
            PayoutConfigurationService payoutConfigurationService) {
        this.payoutConfigurationService = payoutConfigurationService;
    }

    @GetMapping("/configuration")
    public ResponseEntity<ApiResponse<List<PayoutMethodResponse>>> getPayoutConfiguration() {

        List<PayoutMethodResponse> configuration = payoutConfigurationService.getActivePayoutMethods();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payout configuration retrieved successfully",
                        configuration));
    }
}

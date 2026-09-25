package com.veloop.rewards.wallet.controller;

import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.wallet.dto.WalletResponse;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet", description = "Wallet balance and wallet management APIs")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    @Operation(summary = "Get current user's wallet", description = "Returns the wallet belonging to the currently authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            Authentication authentication) {

        Long userId = Long.valueOf(authentication.getName());

        Wallet wallet = walletService.getWallet(userId);

        WalletResponse response = WalletResponse.from(wallet);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Wallet retrieved successfully",
                        response));
    }
}
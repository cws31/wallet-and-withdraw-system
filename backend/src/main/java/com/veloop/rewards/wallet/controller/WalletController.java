package com.veloop.rewards.wallet.controller;

import com.veloop.rewards.common.response.ApiResponse;
import com.veloop.rewards.wallet.dto.WalletResponse;
import com.veloop.rewards.wallet.dto.WalletSummaryResponse;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.veloop.rewards.wallet.dto.WalletTransactionResponse;
import com.veloop.rewards.wallet.service.WalletTransactionService;
import com.veloop.rewards.common.response.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet", description = "Wallet balance and wallet management APIs")
public class WalletController {

        private final WalletService walletService;
        private final WalletTransactionService walletTransactionService;

        public WalletController(
                        WalletService walletService,
                        WalletTransactionService walletTransactionService) {

                this.walletService = walletService;
                this.walletTransactionService = walletTransactionService;
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

        @GetMapping("/transactions")
        @Operation(summary = "Get current user's wallet transactions", description = "Returns paginated wallet transaction history. "
                        + "An optional type filter can restrict the results.", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<ApiResponse<PageResponse<WalletTransactionResponse>>> getTransactions(
                        Authentication authentication,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int limit,
                        @RequestParam(required = false) String type) {

                Long userId = Long.valueOf(authentication.getName());

                int safePage = Math.max(page - 1, 0);
                int safeLimit = Math.min(Math.max(limit, 1), 100);

                PageRequest pageable = PageRequest.of(
                                safePage,
                                safeLimit,
                                Sort.by(Sort.Direction.DESC, "createdAt"));

                PageResponse<WalletTransactionResponse> transactions;

                if (type == null || type.isBlank() || type.equalsIgnoreCase("ALL")) {
                        transactions = walletTransactionService.getTransactions(
                                        userId,
                                        pageable);
                } else {
                        List<TransactionType> transactionTypes = switch (type.trim().toUpperCase()) {
                                case "REWARDS" -> List.of(
                                                TransactionType.REWARD,
                                                TransactionType.BONUS,
                                                TransactionType.REFERRAL,
                                                TransactionType.DAILY_REWARD,
                                                TransactionType.AD_REWARD,
                                                TransactionType.GAME_REWARD);
                                case "WITHDRAWALS" -> List.of(
                                                TransactionType.WITHDRAWAL);
                                default -> {
                                        try {
                                                yield List.of(TransactionType.valueOf(
                                                                type.trim().toUpperCase()));
                                        } catch (IllegalArgumentException exception) {
                                                throw new IllegalArgumentException(
                                                                "Unsupported transaction type: " + type);
                                        }
                                }
                        };

                        transactions = walletTransactionService.getTransactionsByTypes(
                                        userId,
                                        transactionTypes,
                                        pageable);
                }

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Wallet transactions retrieved successfully",
                                                transactions));
        }

        @GetMapping("/summary")
        @Operation(summary = "Get current user's wallet summary", description = "Returns wallet balances and total transaction count for the currently authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<ApiResponse<WalletSummaryResponse>> getWalletSummary(
                        Authentication authentication) {

                Long userId = Long.valueOf(authentication.getName());

                WalletSummaryResponse response = walletService.getWalletSummary(userId);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Wallet summary retrieved successfully",
                                                response));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/credit")
        @Operation(summary = "Credit wallet", description = "Credits a user's wallet. This endpoint is restricted to administrators.", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<ApiResponse<WalletResponse>> creditWallet(
                        Authentication authentication,
                        @Valid @RequestBody WalletCreditRequest request) {

                Long userId = Long.valueOf(authentication.getName());

                Wallet wallet = walletService.creditWallet(userId, request);

                WalletResponse response = WalletResponse.from(wallet);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Wallet credited successfully",
                                                response));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/debit")
        @Operation(summary = "Debit wallet", description = "Debits a user's wallet. This endpoint is restricted to administrators.", security = @SecurityRequirement(name = "bearerAuth"))
        public ResponseEntity<ApiResponse<WalletResponse>> debitWallet(
                        Authentication authentication,
                        @Valid @RequestBody WalletDebitRequest request) {

                Long userId = Long.valueOf(authentication.getName());

                Wallet wallet = walletService.debitWallet(
                                userId,
                                request);

                WalletResponse response = WalletResponse.from(wallet);

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "Wallet debited successfully",
                                                response));
        }
}
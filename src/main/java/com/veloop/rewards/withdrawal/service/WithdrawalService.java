package com.veloop.rewards.withdrawal.service;

import com.veloop.rewards.common.exception.WalletNotFoundException;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final PayoutMethodRepository payoutMethodRepository;
    private final PayoutOptionRepository payoutOptionRepository;
    private final WalletService walletService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    public WithdrawalService(
            WithdrawalRepository withdrawalRepository,
            PayoutMethodRepository payoutMethodRepository,
            PayoutOptionRepository payoutOptionRepository,
            WalletService walletService,
            WalletTransactionRepository walletTransactionRepository,
            UserRepository userRepository) {

        this.withdrawalRepository = withdrawalRepository;
        this.payoutMethodRepository = payoutMethodRepository;
        this.payoutOptionRepository = payoutOptionRepository;
        this.walletService = walletService;
        this.walletTransactionRepository = walletTransactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WithdrawalResponse createWithdrawal(
            Long userId,
            WithdrawalCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PayoutMethod payoutMethod = payoutMethodRepository
                .findById(request.getPayoutMethodId())
                .orElseThrow(() -> new IllegalArgumentException("Payout method not found"));

        if (!Boolean.TRUE.equals(payoutMethod.getActive())) {
            throw new IllegalArgumentException(
                    "Selected payout method is inactive");
        }

        PayoutOption payoutOption = payoutOptionRepository
                .findById(request.getPayoutOptionId())
                .orElseThrow(() -> new IllegalArgumentException("Payout option not found"));

        if (!Boolean.TRUE.equals(payoutOption.getActive())) {
            throw new IllegalArgumentException(
                    "Selected payout option is inactive");
        }

        if (!payoutOption.getMethod().getId()
                .equals(payoutMethod.getId())) {

            throw new IllegalArgumentException(
                    "Payout option does not belong to selected payout method");
        }

        BigDecimal payoutAmount = payoutOption.getPayoutAmount();
        BigDecimal vesRequired = payoutOption.getCurrencyAmount();

        String withdrawalId = "WD-" + UUID.randomUUID();

        WalletDebitRequest debitRequest = new WalletDebitRequest(
                Currency.VES,
                vesRequired,
                TransactionType.WITHDRAWAL,
                "WITHDRAWAL",
                withdrawalId,
                "Wallet withdrawal",
                null);

        walletService.debitWallet(
                userId,
                debitRequest);

        WalletTransaction walletTransaction = walletTransactionRepository
                .findByReferenceId(withdrawalId)
                .orElseThrow(() -> new IllegalStateException(
                        "Withdrawal wallet transaction was not created"));

        Withdrawal withdrawal = new Withdrawal();

        withdrawal.setWithdrawalId(withdrawalId);
        withdrawal.setUser(user);
        withdrawal.setPayoutMethod(payoutMethod);
        withdrawal.setPayoutOption(payoutOption);

        withdrawal.setCurrency(
                payoutOption.getCurrency());

        withdrawal.setCurrencyAmount(
                payoutAmount);

        withdrawal.setPayoutAmount(
                vesRequired);

        withdrawal.setPayoutDetails(
                request.getPayoutDetails());

        withdrawal.setStatus(
                WithdrawalStatus.PENDING);

        withdrawal.setTransaction(walletTransaction);

        LocalDateTime now = LocalDateTime.now();

        withdrawal.setRequestedAt(now);
        withdrawal.setCreatedAt(now);
        withdrawal.setUpdatedAt(now);

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return toResponse(savedWithdrawal);
    }

    @Transactional(readOnly = true)
    public com.veloop.rewards.common.response.PageResponse<WithdrawalResponse> getWithdrawals(
            Long userId,
            org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.domain.Page<WithdrawalResponse> page = withdrawalRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);

        return new com.veloop.rewards.common.response.PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }

    @Transactional(readOnly = true)
    public WithdrawalResponse getWithdrawal(
            Long userId,
            String withdrawalId) {

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Withdrawal not found"));

        if (!withdrawal.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Withdrawal does not belong to the authenticated user");
        }

        return toResponse(withdrawal);
    }

    private WithdrawalResponse toResponse(
            Withdrawal withdrawal) {

        WithdrawalResponse response = new WithdrawalResponse();

        response.setWithdrawalId(
                withdrawal.getWithdrawalId());

        response.setPayoutMethodId(
                withdrawal.getPayoutMethod().getId());

        response.setPayoutMethod(
                withdrawal.getPayoutMethod().getName());

        response.setPayoutOptionId(
                withdrawal.getPayoutOption().getId());

        response.setCurrency(
                withdrawal.getCurrency());

        response.setCurrencyAmount(
                withdrawal.getCurrencyAmount());

        response.setPayoutAmount(
                withdrawal.getPayoutAmount());

        response.setPayoutDetails(
                withdrawal.getPayoutDetails());

        response.setStatus(
                withdrawal.getStatus().name());

        response.setRejectionReason(
                withdrawal.getRejectionReason());

        response.setReviewNote(
                withdrawal.getReviewNote());

        if (withdrawal.getTransaction() != null) {
            response.setTransactionId(
                    withdrawal.getTransaction().getId());
        }

        response.setRequestedAt(
                withdrawal.getRequestedAt());

        response.setProcessedAt(
                withdrawal.getProcessedAt());

        response.setCreatedAt(
                withdrawal.getCreatedAt());

        response.setUpdatedAt(
                withdrawal.getUpdatedAt());

        return response;
    }
}
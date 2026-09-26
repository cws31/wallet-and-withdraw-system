package com.veloop.rewards.withdrawal.service;

import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.common.exception.InvalidWithdrawalStateException;
import com.veloop.rewards.common.exception.WithdrawalNotFoundException;
import com.veloop.rewards.common.exception.WithdrawalOwnershipException;
import com.veloop.rewards.common.exception.WithdrawalTransactionException;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.dto.WalletDebitRequest;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                .orElseThrow(() -> new InvalidWithdrawalRequestException("User not found"));

        PayoutMethod payoutMethod = payoutMethodRepository
                .findById(request.getPayoutMethodId())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("Payout method not found"));

        if (!Boolean.TRUE.equals(payoutMethod.getActive())) {
            throw new InvalidWithdrawalRequestException(
                    "Selected payout method is inactive");
        }

        PayoutOption payoutOption = payoutOptionRepository
                .findById(request.getPayoutOptionId())
                .orElseThrow(() -> new InvalidWithdrawalRequestException("Payout option not found"));

        if (!Boolean.TRUE.equals(payoutOption.getActive())) {
            throw new IllegalArgumentException(
                    "Selected payout option is inactive");
        }

        if (!payoutOption.getMethod().getId()
                .equals(payoutMethod.getId())) {

            throw new InvalidWithdrawalRequestException(
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
                .orElseThrow(() -> new WithdrawalTransactionException(
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
                request.getPayoutDetails().trim());

        withdrawal.setStatus(
                WithdrawalStatus.PENDING);

        withdrawal.setTransaction(
                walletTransaction);

        LocalDateTime now = LocalDateTime.now();

        withdrawal.setRequestedAt(now);
        withdrawal.setCreatedAt(now);
        withdrawal.setUpdatedAt(now);

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return toResponse(savedWithdrawal);
    }

    @Transactional(readOnly = true)
    public PageResponse<WithdrawalResponse> getWithdrawals(
            Long userId,
            Pageable pageable) {

        Page<WithdrawalResponse> page = withdrawalRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId,
                        pageable)
                .map(this::toResponse);

        return new PageResponse<>(
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
                .orElseThrow(() -> new WithdrawalNotFoundException(
                        withdrawalId));

        if (!withdrawal.getUser().getId().equals(userId)) {
            throw new WithdrawalOwnershipException();
        }

        return toResponse(withdrawal);
    }

    @Transactional
    public WithdrawalResponse markProcessing(
            String withdrawalId) {

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow(() -> new WithdrawalNotFoundException(
                        withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new InvalidWithdrawalStateException(
                    "Only PENDING withdrawals can be moved to PROCESSING");
        }

        withdrawal.setStatus(
                WithdrawalStatus.PROCESSING);

        withdrawal.setUpdatedAt(
                LocalDateTime.now());

        return toResponse(
                withdrawalRepository.save(withdrawal));
    }

    @Transactional
    public WithdrawalResponse approveWithdrawal(
            String withdrawalId) {

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow(() -> new WithdrawalNotFoundException(
                        withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING
                && withdrawal.getStatus() != WithdrawalStatus.PROCESSING) {

            throw new InvalidWithdrawalStateException(
                    "Only PENDING or PROCESSING withdrawals can be approved");
        }

        LocalDateTime now = LocalDateTime.now();

        withdrawal.setStatus(
                WithdrawalStatus.APPROVED);

        withdrawal.setProcessedAt(now);
        withdrawal.setUpdatedAt(now);

        return toResponse(
                withdrawalRepository.save(withdrawal));
    }

    @Transactional
    public WithdrawalResponse rejectWithdrawal(
            String withdrawalId,
            String rejectionReason,
            String reviewNote) {

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow(() -> new WithdrawalNotFoundException(
                        withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING
                && withdrawal.getStatus() != WithdrawalStatus.PROCESSING) {

            throw new InvalidWithdrawalStateException(
                    "Only PENDING or PROCESSING withdrawals can be rejected");
        }

        if (rejectionReason == null
                || rejectionReason.isBlank()) {

            throw new InvalidWithdrawalRequestException(
                    "Rejection reason is required");
        }

        String cleanRejectionReason = rejectionReason.trim();

        String cleanReviewNote = reviewNote == null
                ? null
                : reviewNote.trim();

        WalletTransaction originalTransaction = withdrawal.getTransaction();

        if (originalTransaction == null) {
            throw new WithdrawalTransactionException(
                    "Original withdrawal transaction not found");
        }

        BigDecimal reversalAmount = originalTransaction.getAmount();

        if (originalTransaction.getCurrency() != Currency.VES) {

            throw new WithdrawalTransactionException(
                    "Withdrawal transaction currency is not VES");
        }

        String reversalReference = withdrawal.getWithdrawalId()
                + "-REVERSAL";

        WalletCreditRequest reversalRequest = new WalletCreditRequest(
                Currency.VES,
                reversalAmount,
                TransactionType.CORRECTION,
                "WITHDRAWAL_REJECTED",
                reversalReference,
                "Withdrawal rejected - balance reversal",
                null);

        walletService.creditWallet(
                withdrawal.getUser().getId(),
                reversalRequest);

        LocalDateTime now = LocalDateTime.now();

        withdrawal.setStatus(
                WithdrawalStatus.REJECTED);

        withdrawal.setRejectionReason(
                cleanRejectionReason);

        withdrawal.setReviewNote(
                cleanReviewNote);

        withdrawal.setProcessedAt(now);
        withdrawal.setUpdatedAt(now);

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return toResponse(savedWithdrawal);
    }

    @Transactional
    public WithdrawalResponse cancelWithdrawal(
            Long userId,
            String withdrawalId) {

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow(() -> new WithdrawalNotFoundException(
                        withdrawalId));

        if (!withdrawal.getUser().getId().equals(userId)) {
            throw new WithdrawalOwnershipException();
        }

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new InvalidWithdrawalStateException(
                    "Only PENDING withdrawals can be cancelled");
        }

        WalletTransaction originalTransaction = withdrawal.getTransaction();

        if (originalTransaction == null) {
            throw new IllegalStateException(
                    "Original withdrawal transaction not found");
        }

        if (originalTransaction.getCurrency() != Currency.VES) {

            throw new IllegalStateException(
                    "Withdrawal transaction currency is not VES");
        }

        BigDecimal reversalAmount = originalTransaction.getAmount();

        String reversalReference = withdrawal.getWithdrawalId()
                + "-CANCELLATION-REVERSAL";

        WalletCreditRequest reversalRequest = new WalletCreditRequest(
                Currency.VES,
                reversalAmount,
                TransactionType.CORRECTION,
                "WITHDRAWAL_CANCELLED",
                reversalReference,
                "Withdrawal cancelled - balance reversal",
                null);

        walletService.creditWallet(
                userId,
                reversalRequest);

        LocalDateTime now = LocalDateTime.now();

        withdrawal.setStatus(
                WithdrawalStatus.CANCELLED);

        withdrawal.setProcessedAt(now);
        withdrawal.setUpdatedAt(now);

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return toResponse(savedWithdrawal);
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

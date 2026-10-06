package com.veloop.rewards.withdrawal;

import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import com.veloop.rewards.fraud.service.FraudRiskEvaluation;
import com.veloop.rewards.fraud.service.FraudRiskService;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class WithdrawalFraudReviewIntegrationTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Autowired
    private PayoutOptionRepository payoutOptionRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @MockitoBean
    private FraudRiskService fraudRiskService;

    private Long testUserId;

    private PayoutMethod upiMethod;

    private PayoutOption payoutOption;

    @BeforeEach
    void setUp() {

        String uniqueId = String.valueOf(System.nanoTime());

        User user = new User();

        user.setEmail(
                "fraud-review-user-"
                        + uniqueId
                        + "@test.com");

        user.setPasswordHash("test-password");
        user.setName("Fraud Review Test User");
        user.setAccountStatus("ACTIVE");
        user.setVerified(true);

        user = userRepository.save(user);

        testUserId = user.getId();

        walletService.createWallet(testUserId);

        upiMethod = payoutMethodRepository
                .findByCode("UPI")
                .orElseGet(() -> {

                    PayoutMethod method = new PayoutMethod();

                    method.setCode("UPI");
                    method.setName("UPI");
                    method.setActive(true);

                    return payoutMethodRepository
                            .save(method);
                });

        payoutOption = new PayoutOption();

        payoutOption.setMethod(upiMethod);

        payoutOption.setPayoutAmount(
                new BigDecimal("10"));

        payoutOption.setCurrency("INR");

        payoutOption.setCurrencyAmount(
                new BigDecimal("2400"));

        payoutOption.setActive(true);

        payoutOption = payoutOptionRepository.save(
                payoutOption);

        FraudRiskEvaluation reviewEvaluation = new FraudRiskEvaluation(
                40,
                FraudRiskDecision.REVIEW,
                List.of(
                        new RiskRuleResult(
                                true,
                                "TEST_REVIEW_RULE",
                                40,
                                "Test fraud rule requires review")));

        when(
                fraudRiskService.evaluate(
                        anyLong(),
                        any(),
                        anyLong()))
                .thenReturn(reviewEvaluation);
    }

    @Test
    void shouldCreatePendingWithdrawalWithoutDebitingWallet() {

        creditWallet(
                testUserId,
                new BigDecimal("5000"),
                "FRAUD-REVIEW-TEST");

        var walletBefore = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        BigDecimal balanceBefore = walletBefore.getVes();

        long transactionCountBefore = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                100))
                .getTotalElements();

        WithdrawalCreateRequest request = new WithdrawalCreateRequest();

        request.setPayoutMethodId(
                upiMethod.getId());

        request.setPayoutOptionId(
                payoutOption.getId());

        request.setPayoutDetails(
                "test@upi");

        WithdrawalResponse response = withdrawalService.createWithdrawal(
                testUserId,
                "FRAUD-REVIEW-IDEMPOTENCY-001",
                request);

        assertNotNull(response);

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(response.getWithdrawalId())
                .orElseThrow();

        assertEquals(
                WithdrawalStatus.PENDING,
                withdrawal.getStatus());

        assertNotNull(
                withdrawal.getReviewNote());

        assertTrue(
                !withdrawal.getReviewNote()
                        .isBlank());

        assertTrue(
                withdrawal.getReviewNote()
                        .contains("40"));

        var walletAfter = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                balanceBefore.compareTo(
                        walletAfter.getVes()));

        long transactionCountAfter = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                100))
                .getTotalElements();

        assertEquals(
                transactionCountBefore,
                transactionCountAfter);
    }

    private void creditWallet(
            Long userId,
            BigDecimal amount,
            String referenceId) {

        WalletCreditRequest request = new WalletCreditRequest(
                Currency.VES,
                amount,
                TransactionType.REWARD,
                "TEST",
                referenceId,
                "Fraud review test wallet credit",
                null);

        walletService.creditWallet(
                userId,
                request);
    }
}

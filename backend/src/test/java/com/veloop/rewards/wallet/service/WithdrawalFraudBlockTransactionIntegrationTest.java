
package com.veloop.rewards.wallet.service;

import com.veloop.rewards.fraud.enums.FraudRiskDecision;
import com.veloop.rewards.fraud.exception.FraudRiskBlockedException;
import com.veloop.rewards.fraud.service.FraudRiskEvaluation;
import com.veloop.rewards.fraud.service.FraudRiskService;
import com.veloop.rewards.fraud.service.RiskRuleResult;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;

import com.veloop.rewards.wallet.entity.Wallet;

import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest
@Transactional
class WithdrawalFraudBlockTransactionIntegrationTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Autowired
    private PayoutOptionRepository payoutOptionRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @MockitoBean
    private FraudRiskService fraudRiskService;

    private Long testUserId;
    private Long payoutOptionId;

    @BeforeEach
    void setUp() {

        User user = new User();

        user.setName("Fraud Block Test User");

        user.setEmail(
                "fraud-block-" + System.nanoTime() + "@test.com");

        user.setPasswordHash("test-password");

        user.setAccountStatus("ACTIVE");
        user.setVerified(true);

        user = userRepository.save(user);

        testUserId = user.getId();

        Wallet wallet = walletService.createWallet(testUserId);

        wallet.setVes(new BigDecimal("5000"));

        walletRepository.save(wallet);

        PayoutMethod payoutMethod = payoutMethodRepository
                .findByCode("UPI")
                .orElseThrow(
                        () -> new IllegalStateException(
                                "UPI payout method not found"));

        PayoutOption payoutOption = payoutOptionRepository
                .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                        payoutMethod.getId())
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No active UPI payout option found"));

        payoutOptionId = payoutOption.getId();
    }

    @Test
    void shouldBlockWithdrawalWithoutDebitingWalletOrCreatingWithdrawal() {

        FraudRiskEvaluation blockEvaluation = new FraudRiskEvaluation(
                85,
                FraudRiskDecision.BLOCK,
                List.of(
                        new RiskRuleResult(
                                true,
                                "TEST_BLOCK_RULE",
                                85,
                                "Synthetic fraud signal for transaction-safety test")));

        when(
                fraudRiskService.evaluate(
                        anyLong(),
                        any(),
                        ArgumentMatchers.eq(payoutOptionId)))
                .thenReturn(blockEvaluation);

        Wallet walletBefore = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        BigDecimal balanceBefore = walletBefore.getVes();

        long withdrawalCountBefore = withdrawalRepository.count();

        long transactionCountBefore = walletTransactionRepository.count();

        WithdrawalCreateRequest request = new WithdrawalCreateRequest();

        request.setPayoutMethodId(
                walletRepository
                        .findByUserId(testUserId)
                        .orElseThrow()
                        .getId() == null
                                ? null
                                : payoutMethodRepository
                                        .findByCode("UPI")
                                        .orElseThrow()
                                        .getId());

        request.setPayoutOptionId(payoutOptionId);

        request.setPayoutDetails("test@upi");

        assertThrows(
                FraudRiskBlockedException.class,
                () -> withdrawalService.createWithdrawal(
                        testUserId,
                        "FRAUD-BLOCK-001",
                        request));

        Wallet walletAfter = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        long withdrawalCountAfter = withdrawalRepository.count();

        long transactionCountAfter = walletTransactionRepository.count();

        assertEquals(
                0,
                balanceBefore.compareTo(walletAfter.getVes()),
                "Wallet balance must remain unchanged when fraud blocks withdrawal");

        assertEquals(
                withdrawalCountBefore,
                withdrawalCountAfter,
                "No withdrawal must be created when fraud blocks the request");

        assertEquals(
                transactionCountBefore,
                transactionCountAfter,
                "No wallet transaction must be created when fraud blocks the request");

        verify(
                fraudRiskService,
                times(1))
                .evaluate(
                        anyLong(),
                        any(),
                        ArgumentMatchers.eq(payoutOptionId));
    }
}

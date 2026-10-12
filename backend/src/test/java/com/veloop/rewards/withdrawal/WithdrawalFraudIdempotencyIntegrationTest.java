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
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import com.veloop.rewards.wallet.service.WalletService;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.dto.WithdrawalResponse;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class WithdrawalFraudIdempotencyIntegrationTest {

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

    private PayoutOption tenRupeeOption;

    @BeforeEach
    void setUp() {

        String uniqueId = String.valueOf(System.nanoTime());

        User user = new User();

        user.setEmail(
                "fraud-idempotency-user-" + uniqueId + "@test.com");

        user.setPasswordHash("test-password");
        user.setName("Fraud Idempotency Test User");
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

                    return payoutMethodRepository.save(method);
                });

        tenRupeeOption = new PayoutOption();

        tenRupeeOption.setMethod(upiMethod);
        tenRupeeOption.setPayoutAmount(
                new BigDecimal("10"));
        tenRupeeOption.setCurrency("INR");
        tenRupeeOption.setCurrencyAmount(
                new BigDecimal("2400"));
        tenRupeeOption.setActive(true);

        tenRupeeOption = payoutOptionRepository.save(tenRupeeOption);
    }

    private WithdrawalCreateRequest createRequest() {

        WithdrawalCreateRequest request = new WithdrawalCreateRequest();

        request.setPayoutMethodId(
                upiMethod.getId());

        request.setPayoutOptionId(
                tenRupeeOption.getId());

        request.setPayoutDetails(
                "test@upi");

        return request;
    }

    private void creditWallet() {

        WalletCreditRequest request = new WalletCreditRequest(
                Currency.VES,
                new BigDecimal("5000"),
                TransactionType.REWARD,
                "TEST",
                "FRAUD-IDEMPOTENCY-CREDIT",
                "Test wallet credit",
                null);

        walletService.creditWallet(
                testUserId,
                request);
    }

    @Test
    void shouldEvaluateFraudOnlyOnceForSameIdempotencyKey() {

        creditWallet();

        FraudRiskEvaluation allowEvaluation = new FraudRiskEvaluation(
                10,
                FraudRiskDecision.ALLOW,
                java.util.List.of(
                        RiskRuleResult.notTriggered(
                                "TEST_RULE",
                                "No fraud signal")));

        when(fraudRiskService.evaluate(
                anyLong(),
                any(),
                anyLong()))
                .thenReturn(allowEvaluation);

        Wallet walletBefore = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        BigDecimal balanceBefore = walletBefore.getVes();

        long withdrawalCountBefore = withdrawalRepository.count();

        long transactionCountBefore = walletTransactionRepository.count();

        WithdrawalCreateRequest request = createRequest();

        String idempotencyKey = "FRAUD-IDEMPOTENCY-001";

        WithdrawalResponse first = withdrawalService.createWithdrawal(
                testUserId,
                idempotencyKey,
                request);

        assertNotNull(first);

        WithdrawalResponse second = withdrawalService.createWithdrawal(
                testUserId,
                idempotencyKey,
                request);

        assertNotNull(second);

        assertEquals(
                first.getWithdrawalId(),
                second.getWithdrawalId());

        long withdrawalCountAfter = withdrawalRepository.count();

        long transactionCountAfter = walletTransactionRepository.count();

        Wallet walletAfter = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                withdrawalCountBefore + 1,
                withdrawalCountAfter);

        assertEquals(
                transactionCountBefore + 1,
                transactionCountAfter);

        assertEquals(
                0,
                balanceBefore
                        .subtract(new BigDecimal("2400"))
                        .compareTo(walletAfter.getVes()));

        verify(
                fraudRiskService,
                times(1))
                .evaluate(
                        anyLong(),
                        any(),
                        anyLong());
    }
}

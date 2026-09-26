package com.veloop.rewards.withdrawal;

import com.veloop.rewards.common.exception.InsufficientBalanceException;
import com.veloop.rewards.common.exception.InvalidWithdrawalRequestException;
import com.veloop.rewards.common.exception.InvalidWithdrawalStateException;
import com.veloop.rewards.common.exception.WithdrawalNotFoundException;
import com.veloop.rewards.common.exception.WithdrawalOwnershipException;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionType;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WithdrawalServiceIntegrationTest {

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
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Autowired
    private PayoutOptionRepository payoutOptionRepository;

    private Long testUserId;
    private Long secondUserId;

    private PayoutMethod upiMethod;
    private PayoutOption tenRupeeOption;

    @BeforeEach
    void setUp() {

        String uniqueId = String.valueOf(System.nanoTime());

        User user = new User();
        user.setEmail("withdrawal-user-" + uniqueId + "@test.com");
        user.setPasswordHash("test-password");
        user.setName("Withdrawal Test User");

        User secondUser = new User();
        secondUser.setEmail("withdrawal-user2-" + uniqueId + "@test.com");
        secondUser.setPasswordHash("test-password");
        secondUser.setName("Withdrawal Test User 2");

        User savedUser = userRepository.save(user);
        User savedSecondUser = userRepository.save(secondUser);

        testUserId = savedUser.getId();
        secondUserId = savedSecondUser.getId();

        walletService.createWallet(testUserId);
        walletService.createWallet(secondUserId);

        upiMethod = new PayoutMethod();
        upiMethod.setCode("TEST_UPI_" + uniqueId);
        upiMethod.setName("Test UPI");
        upiMethod.setActive(true);

        upiMethod = payoutMethodRepository.save(upiMethod);

        tenRupeeOption = new PayoutOption();
        tenRupeeOption.setMethod(upiMethod);
        tenRupeeOption.setPayoutAmount(new BigDecimal("10"));
        tenRupeeOption.setCurrency("INR");
        tenRupeeOption.setCurrencyAmount(new BigDecimal("2400"));
        tenRupeeOption.setActive(true);

        tenRupeeOption = payoutOptionRepository.save(tenRupeeOption);
    }

    @Test
    void shouldCreateWithdrawalAndDeductVes() {

        creditVes(testUserId, "10000");

        WithdrawalResponse response = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        assertNotNull(response);
        assertNotNull(response.getWithdrawalId());

        assertEquals(
                WithdrawalStatus.PENDING.name(),
                response.getStatus());

        assertEquals(
                0,
                response.getCurrencyAmount()
                        .compareTo(new BigDecimal("10")));

        assertEquals(
                0,
                response.getPayoutAmount()
                        .compareTo(new BigDecimal("2400")));

        Wallet wallet = walletRepository.findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes()
                        .compareTo(new BigDecimal("7600")));
    }

    @Test
    void shouldCreateWithdrawalLedgerTransaction() {

        creditVes(testUserId, "10000");

        WithdrawalResponse response = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        List<WalletTransaction> transactions = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                20))
                .getContent();

        WalletTransaction withdrawalTransaction = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                .findFirst()
                .orElseThrow();

        assertEquals(
                response.getWithdrawalId(),
                withdrawalTransaction.getReferenceId());

        assertEquals(
                0,
                withdrawalTransaction.getAmount()
                        .compareTo(new BigDecimal("2400")));

        assertEquals(
                Currency.VES,
                withdrawalTransaction.getCurrency());

        assertEquals(
                0,
                withdrawalTransaction.getBalanceBefore()
                        .compareTo(new BigDecimal("10000")));

        assertEquals(
                0,
                withdrawalTransaction.getBalanceAfter()
                        .compareTo(new BigDecimal("7600")));
    }

    @Test
    void shouldRejectWithdrawalWhenBalanceIsInsufficient() {

        creditVes(testUserId, "1000");

        assertThrows(
                InsufficientBalanceException.class,
                () -> withdrawalService.createWithdrawal(
                        testUserId,
                        createRequest(
                                upiMethod.getId(),
                                tenRupeeOption.getId(),
                                "test@upi")));

        Wallet wallet = walletRepository.findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes()
                        .compareTo(new BigDecimal("1000")));

        assertEquals(
                0,
                withdrawalRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                testUserId,
                                org.springframework.data.domain.PageRequest.of(
                                        0,
                                        20))
                        .getTotalElements());
    }

    @Test
    void shouldRejectInactivePayoutMethod() {

        upiMethod.setActive(false);
        payoutMethodRepository.save(upiMethod);

        creditVes(testUserId, "10000");

        assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> withdrawalService.createWithdrawal(
                        testUserId,
                        createRequest(
                                upiMethod.getId(),
                                tenRupeeOption.getId(),
                                "test@upi")));
    }

    @Test
    void shouldRejectInactivePayoutOption() {

        tenRupeeOption.setActive(false);
        payoutOptionRepository.save(tenRupeeOption);

        creditVes(testUserId, "10000");

        assertThrows(
                IllegalArgumentException.class,
                () -> withdrawalService.createWithdrawal(
                        testUserId,
                        createRequest(
                                upiMethod.getId(),
                                tenRupeeOption.getId(),
                                "test@upi")));
    }

    @Test
    void shouldRejectPayoutOptionFromDifferentMethod() {

        String uniqueId = String.valueOf(System.nanoTime());

        PayoutMethod secondMethodToSave = new PayoutMethod();

        secondMethodToSave.setCode("TEST_SECOND_" + uniqueId);
        secondMethodToSave.setName("Second Test Method");
        secondMethodToSave.setActive(true);

        PayoutMethod secondMethod = payoutMethodRepository.save(secondMethodToSave);

        PayoutOption secondOptionToSave = new PayoutOption();

        secondOptionToSave.setMethod(secondMethod);
        secondOptionToSave.setPayoutAmount(new BigDecimal("25"));
        secondOptionToSave.setCurrency("INR");
        secondOptionToSave.setCurrencyAmount(new BigDecimal("5800"));
        secondOptionToSave.setActive(true);

        PayoutOption secondOption = payoutOptionRepository.save(secondOptionToSave);

        creditVes(testUserId, "10000");

        assertThrows(
                InvalidWithdrawalRequestException.class,
                () -> withdrawalService.createWithdrawal(
                        testUserId,
                        createRequest(
                                upiMethod.getId(),
                                secondOption.getId(),
                                "test@upi")));
    }

    @Test
    void shouldMoveWithdrawalToProcessing() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        WithdrawalResponse processing = withdrawalService.markProcessing(
                created.getWithdrawalId());

        assertEquals(
                WithdrawalStatus.PROCESSING.name(),
                processing.getStatus());
    }

    @Test
    void shouldApproveWithdrawal() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        WithdrawalResponse approved = withdrawalService.approveWithdrawal(
                created.getWithdrawalId());

        assertEquals(
                WithdrawalStatus.APPROVED.name(),
                approved.getStatus());

        assertNotNull(
                approved.getProcessedAt());

        Wallet wallet = walletRepository.findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes()
                        .compareTo(new BigDecimal("7600")));
    }

    @Test
    void shouldRejectWithdrawalAndReverseVes() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        WithdrawalResponse rejected = withdrawalService.rejectWithdrawal(
                created.getWithdrawalId(),
                "  Invalid payout details  ",
                "  Test review note  ");

        assertEquals(
                WithdrawalStatus.REJECTED.name(),
                rejected.getStatus());

        assertEquals(
                "Invalid payout details",
                rejected.getRejectionReason());

        assertEquals(
                "Test review note",
                rejected.getReviewNote());

        Wallet wallet = walletRepository.findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes()
                        .compareTo(new BigDecimal("10000")));

        List<WalletTransaction> transactions = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                20))
                .getContent();

        assertTrue(
                transactions.stream()
                        .anyMatch(transaction -> transaction.getReferenceId()
                                .equals(
                                        created.getWithdrawalId()
                                                + "-REVERSAL")));
    }

    @Test
    void shouldCancelWithdrawalAndReverseVes() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        WithdrawalResponse cancelled = withdrawalService.cancelWithdrawal(
                testUserId,
                created.getWithdrawalId());

        assertEquals(
                WithdrawalStatus.CANCELLED.name(),
                cancelled.getStatus());

        Wallet wallet = walletRepository.findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                wallet.getVes()
                        .compareTo(new BigDecimal("10000")));

        List<WalletTransaction> transactions = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                20))
                .getContent();

        assertTrue(
                transactions.stream()
                        .anyMatch(transaction -> transaction.getReferenceId()
                                .equals(
                                        created.getWithdrawalId()
                                                + "-CANCELLATION-REVERSAL")));
    }

    @Test
    void shouldRejectInvalidWithdrawalState() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        withdrawalService.cancelWithdrawal(
                testUserId,
                created.getWithdrawalId());

        assertThrows(
                InvalidWithdrawalStateException.class,
                () -> withdrawalService.cancelWithdrawal(
                        testUserId,
                        created.getWithdrawalId()));
    }

    @Test
    void shouldRejectWithdrawalFromAnotherUser() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        assertThrows(
                WithdrawalOwnershipException.class,
                () -> withdrawalService.getWithdrawal(
                        secondUserId,
                        created.getWithdrawalId()));

        assertThrows(
                WithdrawalOwnershipException.class,
                () -> withdrawalService.cancelWithdrawal(
                        secondUserId,
                        created.getWithdrawalId()));
    }

    @Test
    void shouldThrowNotFoundForInvalidWithdrawalId() {

        assertThrows(
                WithdrawalNotFoundException.class,
                () -> withdrawalService.getWithdrawal(
                        testUserId,
                        "WD-DOES-NOT-EXIST"));
    }

    @Test
    void shouldNotRejectApprovedWithdrawal() {

        creditVes(testUserId, "10000");

        WithdrawalResponse created = withdrawalService.createWithdrawal(
                testUserId,
                createRequest(
                        upiMethod.getId(),
                        tenRupeeOption.getId(),
                        "test@upi"));

        withdrawalService.approveWithdrawal(
                created.getWithdrawalId());

        assertThrows(
                InvalidWithdrawalStateException.class,
                () -> withdrawalService.rejectWithdrawal(
                        created.getWithdrawalId(),
                        "Invalid payout details",
                        "Test note"));
    }

    private void creditVes(
            Long userId,
            String amount) {

        walletService.creditWallet(
                userId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal(amount),
                        TransactionType.REWARD,
                        "WITHDRAWAL_TEST",
                        "TEST-CREDIT-" + System.nanoTime(),
                        "Withdrawal integration test credit",
                        null));
    }

    private WithdrawalCreateRequest createRequest(
            Long payoutMethodId,
            Long payoutOptionId,
            String payoutDetails) {

        WithdrawalCreateRequest request = new WithdrawalCreateRequest();

        request.setPayoutMethodId(payoutMethodId);
        request.setPayoutOptionId(payoutOptionId);
        request.setPayoutDetails(payoutDetails);

        return request;
    }
}

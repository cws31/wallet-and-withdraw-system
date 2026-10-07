
package com.veloop.rewards.payoutprocessing.worker;

import com.veloop.rewards.audit.entity.AuditLog;
import com.veloop.rewards.audit.entity.WithdrawalAudit;
import com.veloop.rewards.audit.repository.AuditLogRepository;
import com.veloop.rewards.audit.repository.WithdrawalAuditRepository;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.provider.PayoutProvider;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderRegistry;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderResult;
import com.veloop.rewards.payoutprocessing.provider.PayoutProviderStatus;
import com.veloop.rewards.payoutprocessing.queue.DatabasePayoutQueue;
import com.veloop.rewards.payoutprocessing.repository.PayoutJobRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.dto.WalletCreditRequest;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.entity.WalletTransaction;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class PayoutFailureHandlingIntegrationTest {

    @Autowired
    private PayoutWorker payoutWorker;

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private DatabasePayoutQueue payoutQueue;

    @Autowired
    private PayoutJobRepository payoutJobRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PayoutMethodRepository payoutMethodRepository;

    @Autowired
    private PayoutOptionRepository payoutOptionRepository;

    @Autowired
    private WithdrawalAuditRepository withdrawalAuditRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PayoutProviderRegistry payoutProviderRegistry;

    private Long testUserId;

    private Long payoutJobId;

    private String withdrawalId;

    private PayoutMethod upiMethod;

    private PayoutOption tenRupeeOption;

    @BeforeEach
    void setUp() {

        String uniqueId = UUID.randomUUID().toString();

        User user = new User();

        user.setEmail(
                "payout-failure-"
                        + uniqueId
                        + "@test.com");

        user.setPasswordHash(
                "test-password");

        user.setName(
                "Payout Failure Test User");

        user.setAccountStatus("ACTIVE");

        user.setVerified(true);

        user = userRepository.saveAndFlush(user);

        testUserId = user.getId();

        walletService.createWallet(
                testUserId);

        upiMethod = payoutMethodRepository
                .findByCode("UPI")
                .orElseThrow(() -> new IllegalStateException(
                        "UPI payout method not found"));

        tenRupeeOption = new PayoutOption();

        tenRupeeOption.setMethod(
                upiMethod);

        tenRupeeOption.setPayoutAmount(
                new BigDecimal("10"));

        tenRupeeOption.setCurrency(
                "INR");

        tenRupeeOption.setCurrencyAmount(
                new BigDecimal("2400"));

        tenRupeeOption.setActive(true);

        tenRupeeOption = payoutOptionRepository
                .saveAndFlush(
                        tenRupeeOption);

        walletService.creditWallet(
                testUserId,
                new WalletCreditRequest(
                        Currency.VES,
                        new BigDecimal("5000"),
                        TransactionType.REWARD,
                        "TEST",
                        "PAYOUT-FAILURE-SETUP-"
                                + uniqueId,
                        "Payout failure test setup",
                        null));

        WithdrawalCreateRequest request = new WithdrawalCreateRequest();

        request.setPayoutMethodId(
                upiMethod.getId());

        request.setPayoutOptionId(
                tenRupeeOption.getId());

        request.setPayoutDetails(
                "9876543210@ybl");

        WithdrawalResponse response = withdrawalService.createWithdrawal(
                testUserId,
                "payout-failure-"
                        + uniqueId,
                request);

        assertNotNull(response);

        withdrawalId = response.getWithdrawalId();

        Withdrawal withdrawal = withdrawalRepository
                .findByWithdrawalId(
                        withdrawalId)
                .orElseThrow();

        assertEquals(
                WithdrawalStatus.PENDING,
                withdrawal.getStatus());

        PayoutJob job = payoutQueue.enqueue(
                withdrawal.getId());

        assertNotNull(job);

        payoutJobId = job.getId();
    }

    @AfterEach
    void tearDown() {

        if (testUserId == null) {
            return;
        }

        jdbcTemplate.update(
                """
                        DELETE FROM payout_jobs
                        WHERE withdrawal_id IN (
                            SELECT id
                            FROM withdrawals
                            WHERE user_id = ?
                        )
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM withdrawal_idempotency
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM withdrawal_audit
                        WHERE withdrawal_id IN (
                            SELECT id
                            FROM withdrawals
                            WHERE user_id = ?
                        )
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM audit_log
                        WHERE target_user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM audit_log
                        WHERE actor_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM fraud_risk_events
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM withdrawals
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM wallet_transactions
                        WHERE user_id = ?
                        """,
                testUserId);

        jdbcTemplate.update(
                """
                        DELETE FROM wallets
                        WHERE user_id = ?
                        """,
                testUserId);

        if (tenRupeeOption != null
                && tenRupeeOption.getId() != null) {

            jdbcTemplate.update(
                    """
                            DELETE FROM payout_options
                            WHERE id = ?
                            """,
                    tenRupeeOption.getId());
        }

        jdbcTemplate.update(
                """
                        DELETE FROM users
                        WHERE id = ?
                        """,
                testUserId);
    }

    @Test
    void shouldRejectWithdrawalReverseWalletAndCreateAuditRecords() {

        Withdrawal withdrawalBeforeProcessing = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow();

        assertEquals(
                WithdrawalStatus.PENDING,
                withdrawalBeforeProcessing.getStatus());

        BigDecimal withdrawalAmount = withdrawalBeforeProcessing.getPayoutAmount();

        assertNotNull(withdrawalAmount);

        assertEquals(
                0,
                new BigDecimal("2400")
                        .compareTo(withdrawalAmount),
                "Unexpected withdrawal VES amount");

        Wallet walletAfterWithdrawal = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        BigDecimal expectedBalanceAfterWithdrawal = new BigDecimal("5000")
                .subtract(withdrawalAmount);

        assertEquals(
                0,
                expectedBalanceAfterWithdrawal
                        .compareTo(
                                walletAfterWithdrawal.getVes()),
                "Wallet balance after withdrawal is incorrect");

        assertEquals(
                0,
                withdrawalAmount
                        .compareTo(
                                walletAfterWithdrawal
                                        .getWithdrawnVes()),
                "withdrawnVes was not updated correctly");

        PayoutProvider provider = mock(PayoutProvider.class);

        when(
                provider.process(any()))
                .thenReturn(
                        PayoutProviderResult.rejected(
                                "Provider rejected payout"));

        when(
                payoutProviderRegistry.getProvider("UPI"))
                .thenReturn(provider);

        PayoutProviderResult result = payoutWorker.processJob(payoutJobId);

        assertEquals(
                PayoutProviderStatus.REJECTED,
                result.status());

        Withdrawal rejectedWithdrawal = withdrawalRepository
                .findByWithdrawalId(withdrawalId)
                .orElseThrow();

        assertEquals(
                WithdrawalStatus.REJECTED,
                rejectedWithdrawal.getStatus());

        assertEquals(
                "Provider rejected payout",
                rejectedWithdrawal.getRejectionReason());

        assertNotNull(
                rejectedWithdrawal.getProcessedAt());

        PayoutJob failedJob = payoutJobRepository
                .findByWithdrawalId(
                        rejectedWithdrawal.getId())
                .orElseThrow();

        assertEquals(
                PayoutJobStatus.FAILED,
                failedJob.getStatus());

        assertEquals(
                "Provider rejected payout",
                failedJob.getLastError());

        Wallet walletAfterRejection = walletRepository
                .findByUserId(testUserId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("5000")
                        .compareTo(
                                walletAfterRejection.getVes()),
                "Wallet balance was not restored after payout rejection");

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(
                                walletAfterRejection
                                        .getWithdrawnVes()),
                "withdrawnVes was not reversed");

        List<WalletTransaction> transactions = walletTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(
                        testUserId,
                        org.springframework.data.domain.PageRequest
                                .of(0, 20))
                .getContent();

        WalletTransaction withdrawalTransaction = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                .findFirst()
                .orElseThrow();

        assertEquals(
                0,
                withdrawalAmount.compareTo(
                        withdrawalTransaction.getAmount()),
                "Withdrawal transaction amount is incorrect");

        assertEquals(
                withdrawalId,
                withdrawalTransaction.getReferenceId());

        WalletTransaction reversalTransaction = transactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.CORRECTION)
                .filter(transaction -> (withdrawalId + "-REVERSAL")
                        .equals(
                                transaction
                                        .getReferenceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                0,
                withdrawalAmount.compareTo(
                        reversalTransaction.getAmount()),
                "Reversal amount does not match withdrawal amount");

        assertEquals(
                "WITHDRAWAL_REJECTED",
                reversalTransaction.getSource());

        assertEquals(
                withdrawalId + "-REVERSAL",
                reversalTransaction.getReferenceId());

        assertEquals(
                TransactionStatus.COMPLETED,
                reversalTransaction.getStatus());

        List<WithdrawalAudit> withdrawalAudits = withdrawalAuditRepository
                .findByWithdrawalIdOrderByCreatedAtAsc(
                        rejectedWithdrawal.getId());

        WithdrawalAudit rejectionAudit = withdrawalAudits.stream()
                .filter(audit -> "REJECTED".equals(
                        audit.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                WithdrawalStatus.PROCESSING.name(),
                rejectionAudit.getOldStatus());

        assertEquals(
                WithdrawalStatus.REJECTED.name(),
                rejectionAudit.getNewStatus());

        List<AuditLog> auditLogs = auditLogRepository
                .findByTargetTypeAndReferenceIdOrderByCreatedAtDesc(
                        "WITHDRAWAL",
                        withdrawalId);

        AuditLog rejectionLog = auditLogs.stream()
                .filter(audit -> "WITHDRAWAL_REJECTED"
                        .equals(
                                audit.getAction()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                "WITHDRAWAL",
                rejectionLog.getTargetType());

        assertEquals(
                withdrawalId,
                rejectionLog.getReferenceId());

        assertNotNull(
                rejectionLog.getMetadata());

        org.junit.jupiter.api.Assertions.assertTrue(
                rejectionLog
                        .getMetadata()
                        .contains(
                                "source=PAYOUT_PROCESSING"));

        verify(
                provider,
                times(1))
                .process(any());
    }

}

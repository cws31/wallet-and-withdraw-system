package com.veloop.rewards.withdrawal;

import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.entity.PayoutOption;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
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
import com.veloop.rewards.withdrawal.enums.WithdrawalStatus;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import com.veloop.rewards.withdrawal.service.WithdrawalService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        private PayoutMethodRepository payoutMethodRepository;

        @Autowired
        private PayoutOptionRepository payoutOptionRepository;

        @Autowired
        private WithdrawalRepository withdrawalRepository;

        private Long testUserId;
        private Long secondUserId;
        private Long adminUserId;

        private PayoutMethod upiMethod;
        private PayoutOption tenRupeeOption;

        @BeforeEach
        void setUp() {

                String uniqueId = String.valueOf(System.nanoTime());

                /*
                 * Primary withdrawal test user.
                 *
                 * Phase 9 eligibility requires:
                 * 1. accountStatus = ACTIVE
                 * 2. verified = true
                 */
                User user = new User();

                user.setEmail(
                                "withdrawal-user-" + uniqueId + "@test.com");

                user.setPasswordHash("test-password");
                user.setName("Withdrawal Test User");
                user.setAccountStatus("ACTIVE");
                user.setVerified(true);

                user = userRepository.save(user);

                testUserId = user.getId();

                /*
                 * Second user is also created as an eligible user.
                 * This keeps authorization/access-control tests focused
                 * on ownership rather than account eligibility.
                 */
                User secondUser = new User();

                secondUser.setEmail(
                                "withdrawal-user2-" + uniqueId + "@test.com");

                secondUser.setPasswordHash("test-password");
                secondUser.setName("Withdrawal Test User 2");
                secondUser.setAccountStatus("ACTIVE");
                secondUser.setVerified(true);

                secondUser = userRepository.save(secondUser);

                secondUserId = secondUser.getId();

                /*
                 * Admin user.
                 *
                 * Admin operations do not depend on withdrawal eligibility,
                 * but keeping the fixture explicit avoids relying on entity
                 * defaults.
                 */
                User adminUser = new User();

                adminUser.setEmail(
                                "withdrawal-admin-" + uniqueId + "@test.com");

                adminUser.setPasswordHash("test-password");
                adminUser.setName("Withdrawal Test Admin");
                adminUser.setAccountStatus("ACTIVE");
                adminUser.setVerified(true);

                adminUser = userRepository.save(adminUser);

                adminUserId = adminUser.getId();

                walletService.createWallet(testUserId);
                walletService.createWallet(secondUserId);

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

        private WithdrawalCreateRequest createRequest(
                        String payoutDetails) {

                WithdrawalCreateRequest request = new WithdrawalCreateRequest();

                request.setPayoutMethodId(
                                upiMethod.getId());

                request.setPayoutOptionId(
                                tenRupeeOption.getId());

                request.setPayoutDetails(
                                payoutDetails);

                return request;
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
                                "Test wallet credit",
                                null);

                walletService.creditWallet(
                                userId,
                                request);
        }

        @Test
        void shouldRejectWithdrawalAndReverseBalance() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-006");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-006",
                                request);

                Wallet walletAfterWithdrawal = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("2600")
                                                .compareTo(walletAfterWithdrawal.getVes()));

                assertEquals(
                                0,
                                new BigDecimal("2400")
                                                .compareTo(
                                                                walletAfterWithdrawal
                                                                                .getWithdrawnVes()));

                WithdrawalResponse rejected = withdrawalService.rejectWithdrawal(
                                created.getWithdrawalId(),
                                "  Invalid payout details  ",
                                "  Test review note  ",
                                adminUserId);

                assertEquals(
                                WithdrawalStatus.REJECTED.name(),
                                rejected.getStatus());

                Wallet walletAfterRejection = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("5000")
                                                .compareTo(walletAfterRejection.getVes()));

                assertEquals(
                                0,
                                BigDecimal.ZERO
                                                .compareTo(
                                                                walletAfterRejection
                                                                                .getWithdrawnVes()));

                List<WalletTransaction> transactions = walletTransactionRepository
                                .findByUserIdOrderByCreatedAtDesc(
                                                testUserId,
                                                PageRequest.of(0, 20))
                                .getContent();

                WalletTransaction withdrawalTransaction = transactions.stream()
                                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("5000")
                                                .compareTo(
                                                                withdrawalTransaction
                                                                                .getBalanceBefore()));

                assertEquals(
                                0,
                                new BigDecimal("2600")
                                                .compareTo(
                                                                withdrawalTransaction
                                                                                .getBalanceAfter()));

                assertEquals(
                                created.getWithdrawalId(),
                                withdrawalTransaction.getReferenceId());

                WalletTransaction correctionTransaction = transactions.stream()
                                .filter(transaction -> transaction.getTransactionType() == TransactionType.CORRECTION)
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("2400")
                                                .compareTo(
                                                                correctionTransaction
                                                                                .getAmount()));

                assertEquals(
                                0,
                                new BigDecimal("2600")
                                                .compareTo(
                                                                correctionTransaction
                                                                                .getBalanceBefore()));

                assertEquals(
                                0,
                                new BigDecimal("5000")
                                                .compareTo(
                                                                correctionTransaction
                                                                                .getBalanceAfter()));

                assertEquals(
                                "WITHDRAWAL_REJECTED",
                                correctionTransaction.getSource());

                assertEquals(
                                created.getWithdrawalId() + "-REVERSAL",
                                correctionTransaction.getReferenceId());

                assertEquals(
                                TransactionStatus.COMPLETED,
                                correctionTransaction.getStatus());
        }

        @Test
        void shouldGetUserWithdrawals() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-002");

                WithdrawalCreateRequest request = createRequest("test@upi");

                withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-002",
                                request);

                Pageable pageable = PageRequest.of(0, 10);

                var response = withdrawalService.getWithdrawals(
                                testUserId,
                                pageable);

                assertNotNull(response);
        }

        @Test
        void shouldGetSingleWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-003");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-003",
                                request);

                WithdrawalResponse found = withdrawalService.getWithdrawal(
                                testUserId,
                                created.getWithdrawalId());

                assertNotNull(found);

                assertEquals(
                                created.getWithdrawalId(),
                                found.getWithdrawalId());
        }

        @Test
        void shouldMoveWithdrawalToProcessing() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-004");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-004",
                                request);

                WithdrawalResponse processing = withdrawalService.markProcessing(
                                created.getWithdrawalId(),
                                adminUserId);

                assertEquals(
                                WithdrawalStatus.PROCESSING.name(),
                                processing.getStatus());
        }

        @Test
        void shouldApproveWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-005");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-005",
                                request);

                withdrawalService.markProcessing(
                                created.getWithdrawalId(),
                                adminUserId);

                WithdrawalResponse approved = withdrawalService.approveWithdrawal(
                                created.getWithdrawalId(),
                                adminUserId);

                assertEquals(
                                WithdrawalStatus.APPROVED.name(),
                                approved.getStatus());
        }

        @Test
        void shouldCancelPendingWithdrawalAndReverseBalance() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-007");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-007",
                                request);

                WithdrawalResponse cancelled = withdrawalService.cancelWithdrawal(
                                testUserId,
                                created.getWithdrawalId());

                assertEquals(
                                WithdrawalStatus.CANCELLED.name(),
                                cancelled.getStatus());

                Wallet wallet = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("5000")
                                                .compareTo(wallet.getVes()));

                assertEquals(
                                0,
                                BigDecimal.ZERO
                                                .compareTo(wallet.getWithdrawnVes()));

                List<WalletTransaction> transactions = walletTransactionRepository
                                .findByUserIdOrderByCreatedAtDesc(
                                                testUserId,
                                                PageRequest.of(0, 20))
                                .getContent();

                WalletTransaction withdrawalTransaction = transactions.stream()
                                .filter(transaction -> transaction.getTransactionType() == TransactionType.WITHDRAWAL)
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("5000")
                                                .compareTo(
                                                                withdrawalTransaction
                                                                                .getBalanceBefore()));

                assertEquals(
                                0,
                                new BigDecimal("2600")
                                                .compareTo(
                                                                withdrawalTransaction
                                                                                .getBalanceAfter()));

                assertEquals(
                                created.getWithdrawalId(),
                                withdrawalTransaction.getReferenceId());

                WalletTransaction correctionTransaction = transactions.stream()
                                .filter(transaction -> transaction.getTransactionType() == TransactionType.CORRECTION)
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("2400")
                                                .compareTo(
                                                                correctionTransaction
                                                                                .getAmount()));

                assertEquals(
                                0,
                                new BigDecimal("2600")
                                                .compareTo(
                                                                correctionTransaction
                                                                                .getBalanceBefore()));

                assertEquals(
                                0,
                                new BigDecimal("5000")
                                                .compareTo(
                                                                correctionTransaction
                                                                                .getBalanceAfter()));

                assertEquals(
                                "WITHDRAWAL_CANCELLED",
                                correctionTransaction.getSource());

                assertEquals(
                                created.getWithdrawalId()
                                                + "-CANCELLATION-REVERSAL",
                                correctionTransaction.getReferenceId());

                assertEquals(
                                TransactionStatus.COMPLETED,
                                correctionTransaction.getStatus());
        }

        @Test
        void shouldRejectWithdrawalWhenBalanceIsInsufficient() {

                creditWallet(
                                testUserId,
                                new BigDecimal("1000"),
                                "TEST-REF-008");

                WithdrawalCreateRequest request = createRequest("test@upi");

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.createWithdrawal(
                                                testUserId,
                                                "IDEMPOTENCY-008",
                                                request));
        }

        @Test
        void shouldRejectInvalidPayoutOption() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-009");

                WithdrawalCreateRequest request = new WithdrawalCreateRequest();

                request.setPayoutMethodId(
                                upiMethod.getId());

                request.setPayoutOptionId(
                                999999L);

                request.setPayoutDetails(
                                "test@upi");

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.createWithdrawal(
                                                testUserId,
                                                "IDEMPOTENCY-009",
                                                request));
        }

        @Test
        void shouldNotAllowAnotherUserToAccessWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-010");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-010",
                                request);

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.getWithdrawal(
                                                secondUserId,
                                                created.getWithdrawalId()));
        }

        @Test
        void shouldReturnSameWithdrawalForSameIdempotencyKey() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-011");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse first = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-DUPLICATE",
                                request);

                WithdrawalResponse second = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-DUPLICATE",
                                request);

                assertEquals(
                                first.getWithdrawalId(),
                                second.getWithdrawalId());
        }

        @Test
        void shouldRejectDifferentRequestWithSameIdempotencyKey() {

                creditWallet(
                                testUserId,
                                new BigDecimal("10000"),
                                "TEST-REF-012");

                WithdrawalCreateRequest firstRequest = new WithdrawalCreateRequest();

                firstRequest.setPayoutMethodId(
                                upiMethod.getId());

                firstRequest.setPayoutOptionId(
                                tenRupeeOption.getId());

                firstRequest.setPayoutDetails(
                                "first@upi");

                WithdrawalCreateRequest secondRequest = new WithdrawalCreateRequest();

                secondRequest.setPayoutMethodId(
                                upiMethod.getId());

                secondRequest.setPayoutOptionId(
                                tenRupeeOption.getId());

                secondRequest.setPayoutDetails(
                                "second@upi");

                withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-CONFLICT",
                                firstRequest);

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.createWithdrawal(
                                                testUserId,
                                                "IDEMPOTENCY-CONFLICT",
                                                secondRequest));
        }

        @Test
        void shouldNotApproveRejectedWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-013");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-013",
                                request);

                withdrawalService.rejectWithdrawal(
                                created.getWithdrawalId(),
                                "Invalid payout details",
                                "Test rejection",
                                adminUserId);

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.approveWithdrawal(
                                                created.getWithdrawalId(),
                                                adminUserId));
        }

        @Test
        void shouldNotRejectApprovedWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-014");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-014",
                                request);

                withdrawalService.markProcessing(
                                created.getWithdrawalId(),
                                adminUserId);

                withdrawalService.approveWithdrawal(
                                created.getWithdrawalId(),
                                adminUserId);

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.rejectWithdrawal(
                                                created.getWithdrawalId(),
                                                "Invalid payout details",
                                                "Test note",
                                                adminUserId));
        }

        @Test
        void shouldNotCancelProcessingWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-015");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-015",
                                request);

                withdrawalService.markProcessing(
                                created.getWithdrawalId(),
                                adminUserId);

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.cancelWithdrawal(
                                                testUserId,
                                                created.getWithdrawalId()));
        }

        @Test
        void shouldNotProcessApprovedWithdrawalAgain() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-016");

                WithdrawalCreateRequest request = createRequest("test@upi");

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-016",
                                request);

                withdrawalService.markProcessing(
                                created.getWithdrawalId(),
                                adminUserId);

                withdrawalService.approveWithdrawal(
                                created.getWithdrawalId(),
                                adminUserId);

                assertThrows(
                                Exception.class,
                                () -> withdrawalService.markProcessing(
                                                created.getWithdrawalId(),
                                                adminUserId));
        }

        @Test
        void shouldCreateLedgerEntryForWalletCredit() {

                String referenceId = "TEST-CREDIT-LEDGER-001";

                creditWallet(
                                testUserId,
                                new BigDecimal("1000"),
                                referenceId);

                Wallet wallet = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("1000")
                                                .compareTo(wallet.getVes()));

                WalletTransaction transaction = walletTransactionRepository
                                .findByReferenceId(referenceId)
                                .orElseThrow();

                assertNotNull(transaction.getTransactionId());

                assertEquals(
                                testUserId,
                                transaction.getUserId());

                assertEquals(
                                Currency.VES,
                                transaction.getCurrency());

                assertEquals(
                                TransactionType.REWARD,
                                transaction.getTransactionType());

                assertEquals(
                                0,
                                new BigDecimal("1000")
                                                .compareTo(transaction.getAmount()));

                assertEquals(
                                0,
                                BigDecimal.ZERO
                                                .compareTo(transaction.getBalanceBefore()));

                assertEquals(
                                0,
                                new BigDecimal("1000")
                                                .compareTo(transaction.getBalanceAfter()));

                assertEquals(
                                "TEST",
                                transaction.getSource());

                assertEquals(
                                referenceId,
                                transaction.getReferenceId());

                assertEquals(
                                TransactionStatus.COMPLETED,
                                transaction.getStatus());
        }

        @Test
        void shouldOnlyUpdateWithdrawnVesForWithdrawalTransactions() {

                creditWallet(
                                testUserId,
                                new BigDecimal("5000"),
                                "TEST-REF-WITHDRAWN-VES");

                Wallet afterCredit = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                BigDecimal.ZERO
                                                .compareTo(afterCredit.getWithdrawnVes()));

                WithdrawalResponse created = withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-WITHDRAWN-VES",
                                createRequest("test@upi"));

                Wallet afterWithdrawal = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("2400")
                                                .compareTo(
                                                                afterWithdrawal
                                                                                .getWithdrawnVes()));

                withdrawalService.rejectWithdrawal(
                                created.getWithdrawalId(),
                                "Invalid payout details",
                                "Test reversal",
                                adminUserId);

                Wallet afterReversal = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                BigDecimal.ZERO
                                                .compareTo(
                                                                afterReversal
                                                                                .getWithdrawnVes()));
        }

        @Test
        void shouldDeductCorrectVesAmountDuringWithdrawal() {

                creditWallet(
                                testUserId,
                                new BigDecimal("10000"),
                                "TEST-REF-017");

                WithdrawalCreateRequest request = createRequest("test@upi");

                withdrawalService.createWithdrawal(
                                testUserId,
                                "IDEMPOTENCY-017",
                                request);

                Wallet wallet = walletRepository
                                .findByUserId(testUserId)
                                .orElseThrow();

                assertEquals(
                                0,
                                new BigDecimal("7600.0000")
                                                .compareTo(
                                                                wallet.getVes()));
        }
}

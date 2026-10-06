package com.veloop.rewards.reconciliation.service;

import com.veloop.rewards.audit.service.AuditLogService;
import com.veloop.rewards.reconciliation.dto.ReconciliationResult;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.enums.TransactionStatus;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    void shouldReconcileWhenWalletAndLedgerBalancesMatch() {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getVes())
                .thenReturn(new BigDecimal("100.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.VES,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("100.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.VES);

        assertNotNull(result);

        assertEquals(
                new BigDecimal("100.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("100.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO));

        assertTrue(result.reconciled());

        verify(auditLogService, never())
                .record(
                        any(),
                        any(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString());
    }

    @Test
    void shouldDetectMismatchWhenWalletBalanceIsGreaterThanLedgerBalance() {

        Wallet wallet = mock(Wallet.class);
        User user = mock(User.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getVes())
                .thenReturn(new BigDecimal("150.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.VES,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("100.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.VES);

        assertNotNull(result);

        assertEquals(
                new BigDecimal("150.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("100.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                new BigDecimal("50.0000"),
                result.difference());

        assertFalse(result.reconciled());

        verify(auditLogService, times(1))
                .record(
                        isNull(),
                        eq(user),
                        eq("WALLET_RECONCILIATION"),
                        eq("RECONCILIATION_FAILURE"),
                        eq("1"),
                        contains("\"walletId\": 1"));
    }

    @Test
    void shouldDetectMismatchWhenLedgerBalanceIsGreaterThanWalletBalance() {

        Wallet wallet = mock(Wallet.class);
        User user = mock(User.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getVes())
                .thenReturn(new BigDecimal("100.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.VES,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("150.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.VES);

        assertNotNull(result);

        assertEquals(
                new BigDecimal("100.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("150.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                new BigDecimal("-50.0000"),
                result.difference());

        assertFalse(result.reconciled());

        verify(auditLogService, times(1))
                .record(
                        isNull(),
                        eq(user),
                        eq("WALLET_RECONCILIATION"),
                        eq("RECONCILIATION_FAILURE"),
                        eq("1"),
                        contains("\"difference\": \"-50.0000\""));
    }

    @Test
    void shouldTreatNullLedgerBalanceAsZero() {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getVes())
                .thenReturn(BigDecimal.ZERO);

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.VES,
                        TransactionStatus.COMPLETED))
                .thenReturn(null);

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.VES);

        assertNotNull(result);

        assertEquals(
                0,
                result.walletBalance().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                result.ledgerDerivedBalance().compareTo(BigDecimal.ZERO));

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO));

        assertTrue(result.reconciled());

        verify(auditLogService, never())
                .record(
                        any(),
                        any(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString());
    }

    @Test
    void shouldUseCorrectWalletBalanceForSves() {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getSves())
                .thenReturn(new BigDecimal("200.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.SVES,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("200.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.SVES);

        assertEquals(
                new BigDecimal("200.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("200.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO));

        assertTrue(result.reconciled());
    }

    @Test
    void shouldUseCorrectWalletBalanceForGems() {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getGems())
                .thenReturn(new BigDecimal("300.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.GEMS,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("300.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.GEMS);

        assertEquals(
                new BigDecimal("300.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("300.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO));

        assertTrue(result.reconciled());
    }

    @Test
    void shouldUseCorrectWalletBalanceForTokens() {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getTokens())
                .thenReturn(new BigDecimal("400.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.TOKENS,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("400.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.TOKENS);

        assertEquals(
                new BigDecimal("400.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("400.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO));

        assertTrue(result.reconciled());
    }

    @Test
    void shouldUseCorrectWalletBalanceForSpins() {

        Wallet wallet = mock(Wallet.class);

        when(wallet.getId()).thenReturn(1L);
        when(wallet.getUserId()).thenReturn(10L);
        when(wallet.getSpins())
                .thenReturn(new BigDecimal("500.0000"));

        when(walletRepository.findById(1L))
                .thenReturn(Optional.of(wallet));

        when(walletTransactionRepository
                .sumBalanceDeltaByWalletIdAndCurrencyAndStatus(
                        1L,
                        Currency.SPINS,
                        TransactionStatus.COMPLETED))
                .thenReturn(new BigDecimal("500.0000"));

        ReconciliationResult result = reconciliationService.reconcile(
                1L,
                Currency.SPINS);

        assertEquals(
                new BigDecimal("500.0000"),
                result.walletBalance());

        assertEquals(
                new BigDecimal("500.0000"),
                result.ledgerDerivedBalance());

        assertEquals(
                0,
                result.difference().compareTo(BigDecimal.ZERO));

        assertTrue(result.reconciled());
    }

    @Test
    void shouldFailWhenWalletDoesNotExist() {

        when(walletRepository.findById(999L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reconciliationService.reconcile(
                        999L,
                        Currency.VES));

        assertEquals(
                "Wallet not found: 999",
                exception.getMessage());

        verifyNoInteractions(
                walletTransactionRepository,
                userRepository,
                auditLogService);
    }
}
package com.veloop.rewards.reconciliation;

import com.veloop.rewards.audit.entity.AuditLog;
import com.veloop.rewards.audit.repository.AuditLogRepository;
import com.veloop.rewards.reconciliation.service.ReconciliationService;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.user.repository.UserRepository;
import com.veloop.rewards.wallet.entity.Wallet;
import com.veloop.rewards.wallet.enums.Currency;
import com.veloop.rewards.wallet.repository.WalletRepository;
import com.veloop.rewards.wallet.service.WalletService;
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
class ReconciliationAuditIntegrationTest {

        @Autowired
        private ReconciliationService reconciliationService;

        @Autowired
        private AuditLogRepository auditLogRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private WalletRepository walletRepository;

        @Autowired
        private WalletService walletService;

        private User user;
        private Wallet wallet;

        @BeforeEach
        void setUp() {

                user = User.builder()
                                .name("Reconciliation Audit User")
                                .email(
                                                "reconciliation-audit-"
                                                                + System.nanoTime()
                                                                + "@example.com")
                                .passwordHash("test-password")
                                .role("USER")
                                .accountStatus("ACTIVE")
                                .verified(false)
                                .level(0)
                                .build();

                user = userRepository.saveAndFlush(user);

                wallet = walletService.createWallet(user.getId());

                auditLogRepository.deleteAll();
                auditLogRepository.flush();
        }

        @Test
        void shouldPersistAuditRecordWhenReconciliationFails() {

                wallet.setVes(
                                new BigDecimal("1000"));

                wallet = walletRepository.saveAndFlush(wallet);

                var result = reconciliationService.reconcile(
                                wallet.getId(),
                                Currency.VES);

                assertFalse(result.reconciled());

                assertEquals(
                                0,
                                result.ledgerDerivedBalance()
                                                .compareTo(BigDecimal.ZERO));

                assertEquals(
                                0,
                                result.walletBalance()
                                                .compareTo(new BigDecimal("1000")));

                assertEquals(
                                0,
                                result.difference()
                                                .compareTo(new BigDecimal("1000")));

                List<AuditLog> auditLogs = auditLogRepository
                                .findByTargetTypeAndReferenceIdOrderByCreatedAtDesc(
                                                "WALLET_RECONCILIATION",
                                                wallet.getId().toString());

                assertEquals(
                                1,
                                auditLogs.size());

                AuditLog auditLog = auditLogs.get(0);

                assertEquals(
                                "WALLET_RECONCILIATION",
                                auditLog.getTargetType());

                assertEquals(
                                "RECONCILIATION_FAILURE",
                                auditLog.getAction());

                assertEquals(
                                wallet.getId().toString(),
                                auditLog.getReferenceId());

                assertNotNull(
                                auditLog.getTargetUser());

                assertEquals(
                                user.getId(),
                                auditLog.getTargetUser().getId());

                assertNotNull(
                                auditLog.getMetadata());

                String metadata = auditLog.getMetadata();

                assertTrue(
                                metadata.contains(
                                                "\"walletId\": "
                                                                + wallet.getId()));

                assertTrue(
                                metadata.contains(
                                                "\"currency\": \"VES\""));

                assertTrue(
                                metadata.contains(
                                                "\"walletBalance\": \"1000\""));

                assertTrue(
                                metadata.contains(
                                                "\"ledgerDerivedBalance\": \"0.0000\""));

                assertTrue(
                                metadata.contains(
                                                "\"difference\": \"1000.0000\""));
        }

        @Test
        void shouldNotCreateFailureAuditWhenReconciliationSucceeds() {

                var result = reconciliationService.reconcile(
                                wallet.getId(),
                                Currency.VES);

                assertTrue(result.reconciled());

                List<AuditLog> auditLogs = auditLogRepository
                                .findByTargetTypeAndReferenceIdOrderByCreatedAtDesc(
                                                "WALLET_RECONCILIATION",
                                                wallet.getId().toString());

                assertTrue(auditLogs.isEmpty());
        }
}
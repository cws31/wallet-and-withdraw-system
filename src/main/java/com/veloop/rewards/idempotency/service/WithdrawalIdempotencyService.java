package com.veloop.rewards.idempotency.service;

import com.veloop.rewards.common.exception.IdempotencyConflictException;
import com.veloop.rewards.common.exception.IdempotencyKeyRequiredException;
import com.veloop.rewards.idempotency.entity.WithdrawalIdempotency;
import com.veloop.rewards.idempotency.repository.WithdrawalIdempotencyRepository;
import com.veloop.rewards.user.entity.User;
import com.veloop.rewards.withdrawal.dto.WithdrawalCreateRequest;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class WithdrawalIdempotencyService {

    private final WithdrawalIdempotencyRepository repository;

    public WithdrawalIdempotencyService(
            WithdrawalIdempotencyRepository repository) {

        this.repository = repository;
    }

    public String validateAndNormalizeKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IdempotencyKeyRequiredException();
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > 100) {
            throw new IdempotencyConflictException();
        }

        return normalizedKey;
    }

    public String createRequestFingerprint(
            WithdrawalCreateRequest request) {

        String normalizedPayoutDetails = request.getPayoutDetails() == null
                ? ""
                : request.getPayoutDetails().trim();

        String rawValue = String.valueOf(request.getPayoutMethodId())
                + "|"
                + String.valueOf(request.getPayoutOptionId())
                + "|"
                + normalizedPayoutDetails;

        return sha256(rawValue);
    }

    @Transactional(readOnly = true)
    public Optional<WithdrawalIdempotency> findExisting(
            Long userId,
            String idempotencyKey) {

        return repository.findByUserIdAndIdempotencyKey(
                userId,
                idempotencyKey);
    }

    public void validateFingerprint(
            WithdrawalIdempotency existing,
            String requestFingerprint) {

        if (!existing.getRequestFingerprint()
                .equals(requestFingerprint)) {

            throw new IdempotencyConflictException();
        }
    }

    @Transactional
    public WithdrawalIdempotency createRecord(
            User user,
            String idempotencyKey,
            String requestFingerprint,
            Withdrawal withdrawal) {

        WithdrawalIdempotency record = new WithdrawalIdempotency();

        record.setUser(user);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestFingerprint(requestFingerprint);
        record.setWithdrawal(withdrawal);

        return repository.save(record);
    }

    private String sha256(String value) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    ex);
        }
    }
}
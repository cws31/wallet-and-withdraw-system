
package com.veloop.rewards.payoutprocessing.queue;

import com.veloop.rewards.payoutprocessing.entity.PayoutJob;
import com.veloop.rewards.payoutprocessing.enums.PayoutJobStatus;
import com.veloop.rewards.payoutprocessing.repository.PayoutJobRepository;
import com.veloop.rewards.withdrawal.entity.Withdrawal;
import com.veloop.rewards.withdrawal.repository.WithdrawalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DatabasePayoutQueue
        implements PayoutQueue {

    private final PayoutJobRepository payoutJobRepository;
    private final WithdrawalRepository withdrawalRepository;

    public DatabasePayoutQueue(
            PayoutJobRepository payoutJobRepository,
            WithdrawalRepository withdrawalRepository) {

        this.payoutJobRepository = payoutJobRepository;

        this.withdrawalRepository = withdrawalRepository;
    }

    @Override
    @Transactional
    public PayoutJob enqueue(
            Long withdrawalId) {

        PayoutJob existingJob = payoutJobRepository
                .findByWithdrawalId(withdrawalId)
                .orElse(null);

        if (existingJob != null) {
            return existingJob;
        }

        Withdrawal withdrawal = withdrawalRepository
                .findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Withdrawal not found: "
                                + withdrawalId));

        PayoutJob job = new PayoutJob();

        job.setWithdrawal(withdrawal);
        job.setStatus(PayoutJobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setNextAttemptAt(
                LocalDateTime.now());

        try {

            return payoutJobRepository.saveAndFlush(job);

        } catch (DataIntegrityViolationException exception) {

            return payoutJobRepository
                    .findByWithdrawalId(withdrawalId)
                    .orElseThrow(() -> exception);
        }
    }
}

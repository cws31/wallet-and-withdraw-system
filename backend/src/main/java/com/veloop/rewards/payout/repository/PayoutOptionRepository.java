package com.veloop.rewards.payout.repository;

import com.veloop.rewards.payout.entity.PayoutOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutOptionRepository
        extends JpaRepository<PayoutOption, Long> {

    List<PayoutOption> findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
            Long methodId);

    List<PayoutOption> findByActiveTrueOrderByPayoutAmountAsc();
}

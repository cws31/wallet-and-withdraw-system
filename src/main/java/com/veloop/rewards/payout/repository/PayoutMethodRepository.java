package com.veloop.rewards.payout.repository;

import com.veloop.rewards.payout.entity.PayoutMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayoutMethodRepository
        extends JpaRepository<PayoutMethod, Long> {

    Optional<PayoutMethod> findByCode(String code);

    List<PayoutMethod> findByActiveTrueOrderByNameAsc();
}

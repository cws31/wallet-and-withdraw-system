package com.veloop.rewards.payout.service;

import com.veloop.rewards.payout.dto.PayoutMethodResponse;
import com.veloop.rewards.payout.dto.PayoutOptionResponse;
import com.veloop.rewards.payout.entity.PayoutMethod;
import com.veloop.rewards.payout.repository.PayoutMethodRepository;
import com.veloop.rewards.payout.repository.PayoutOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PayoutConfigurationService {

    private final PayoutMethodRepository payoutMethodRepository;
    private final PayoutOptionRepository payoutOptionRepository;

    public PayoutConfigurationService(
            PayoutMethodRepository payoutMethodRepository,
            PayoutOptionRepository payoutOptionRepository) {

        this.payoutMethodRepository = payoutMethodRepository;
        this.payoutOptionRepository = payoutOptionRepository;
    }

    @Transactional(readOnly = true)
    public List<PayoutMethodResponse> getActivePayoutMethods() {

        List<PayoutMethod> methods = payoutMethodRepository
                .findByActiveTrueOrderByNameAsc();

        return methods.stream()
                .map(method -> {

                    List<PayoutOptionResponse> options = payoutOptionRepository
                            .findByMethodIdAndActiveTrueOrderByPayoutAmountAsc(
                                    method.getId())
                            .stream()
                            .map(PayoutOptionResponse::from)
                            .toList();

                    return PayoutMethodResponse.from(
                            method,
                            options);
                })
                .toList();
    }
}

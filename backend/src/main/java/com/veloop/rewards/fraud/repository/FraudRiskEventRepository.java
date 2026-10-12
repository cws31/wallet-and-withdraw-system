package com.veloop.rewards.fraud.repository;

import com.veloop.rewards.fraud.entity.FraudRiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudRiskEventRepository extends JpaRepository<FraudRiskEvent, Long> {
}

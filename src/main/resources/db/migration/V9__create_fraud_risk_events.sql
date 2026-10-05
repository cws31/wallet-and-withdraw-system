CREATE TABLE fraud_risk_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    withdrawal_id BIGINT,
    risk_score INT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    review_status VARCHAR(20) NOT NULL,
    triggered_rules TEXT,
    explanation TEXT,
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    review_note VARCHAR(1000),
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_fraud_risk_events_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_fraud_risk_events_withdrawal
        FOREIGN KEY (withdrawal_id)
        REFERENCES withdrawals(id),

    CONSTRAINT fk_fraud_risk_events_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id),

    INDEX idx_fraud_risk_events_user_id (user_id),
    INDEX idx_fraud_risk_events_withdrawal_id (withdrawal_id),
    INDEX idx_fraud_risk_events_decision (decision),
    INDEX idx_fraud_risk_events_created_at (created_at)
);

CREATE TABLE fraud_risk_events (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,

    withdrawal_id BIGINT NULL,

    risk_score INT NOT NULL,

    decision VARCHAR(20) NOT NULL,

    review_status VARCHAR(30) NOT NULL,

    triggered_rules TEXT NOT NULL,

    reviewed_by BIGINT NULL,

    reviewed_at DATETIME NULL,

    review_note TEXT NULL,

    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_fraud_risk_event_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_fraud_risk_event_withdrawal
        FOREIGN KEY (withdrawal_id)
        REFERENCES withdrawals(id),

    INDEX idx_fraud_risk_user (user_id),

    INDEX idx_fraud_risk_withdrawal (withdrawal_id),

    INDEX idx_fraud_risk_decision (decision),

    INDEX idx_fraud_risk_review_status (review_status),

    INDEX idx_fraud_risk_created_at (created_at)
);
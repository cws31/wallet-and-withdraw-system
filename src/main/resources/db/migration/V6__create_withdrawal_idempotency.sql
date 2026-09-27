CREATE TABLE withdrawal_idempotency (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT NOT NULL,

    idempotency_key VARCHAR(100) NOT NULL,

    request_fingerprint VARCHAR(64) NOT NULL,

    withdrawal_record_id BIGINT,

    created_at DATETIME NOT NULL,

    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_withdrawal_idempotency_user_key
        UNIQUE (user_id, idempotency_key),

    CONSTRAINT fk_withdrawal_idempotency_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_withdrawal_idempotency_withdrawal
        FOREIGN KEY (withdrawal_record_id)
        REFERENCES withdrawals(id),

    INDEX idx_withdrawal_idempotency_withdrawal_record_id
        (withdrawal_record_id),

    INDEX idx_withdrawal_idempotency_created_at
        (created_at)
);
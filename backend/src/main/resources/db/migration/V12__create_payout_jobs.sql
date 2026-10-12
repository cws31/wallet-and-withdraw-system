CREATE TABLE payout_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,

    withdrawal_id BIGINT NOT NULL,

    status VARCHAR(30) NOT NULL,

    attempt_count INT NOT NULL DEFAULT 0,

    next_attempt_at DATETIME,

    last_error TEXT,

    created_at DATETIME NOT NULL,

    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_payout_jobs_withdrawal_id
        UNIQUE (withdrawal_id),

    CONSTRAINT fk_payout_jobs_withdrawal
        FOREIGN KEY (withdrawal_id)
        REFERENCES withdrawals(id),

    INDEX idx_payout_jobs_status
        (status),

    INDEX idx_payout_jobs_next_attempt_at
        (next_attempt_at),

    INDEX idx_payout_jobs_status_next_attempt
        (status, next_attempt_at)
);

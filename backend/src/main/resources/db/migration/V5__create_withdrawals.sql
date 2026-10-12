CREATE TABLE withdrawals (
    id BIGINT NOT NULL AUTO_INCREMENT,

    withdrawal_id VARCHAR(100) NOT NULL,

    user_id BIGINT NOT NULL,

    payout_method_id BIGINT NOT NULL,

    payout_option_id BIGINT NOT NULL,

    currency VARCHAR(20) NOT NULL,

    currency_amount DECIMAL(19,4) NOT NULL,

    payout_amount DECIMAL(19,4) NOT NULL,

    payout_details TEXT,

    status VARCHAR(30) NOT NULL,

    rejection_reason VARCHAR(500),

    review_note VARCHAR(1000),

    transaction_id BIGINT,

    requested_at DATETIME NOT NULL,

    processed_at DATETIME,

    created_at DATETIME NOT NULL,

    updated_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_withdrawals_withdrawal_id
        UNIQUE (withdrawal_id),

    CONSTRAINT fk_withdrawals_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_withdrawals_payout_method
        FOREIGN KEY (payout_method_id)
        REFERENCES payout_methods(id),

    CONSTRAINT fk_withdrawals_payout_option
        FOREIGN KEY (payout_option_id)
        REFERENCES payout_options(id),

    CONSTRAINT fk_withdrawals_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES wallet_transactions(id),

    INDEX idx_withdrawals_user_id (user_id),

    INDEX idx_withdrawals_status (status),

    INDEX idx_withdrawals_created_at (created_at),

    INDEX idx_withdrawals_user_status (user_id, status),

    INDEX idx_withdrawals_payout_option_id (payout_option_id)
);
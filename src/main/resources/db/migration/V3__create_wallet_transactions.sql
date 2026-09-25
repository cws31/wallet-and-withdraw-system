CREATE TABLE wallet_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,

    transaction_id VARCHAR(64) NOT NULL,

    user_id BIGINT NOT NULL,

    wallet_id BIGINT NOT NULL,

    currency VARCHAR(20) NOT NULL,

    transaction_type VARCHAR(50) NOT NULL,

    amount DECIMAL(19,4) NOT NULL,

    balance_before DECIMAL(19,4) NOT NULL,

    balance_after DECIMAL(19,4) NOT NULL,

    source VARCHAR(100) NOT NULL,

    reference_id VARCHAR(100),

    status VARCHAR(30) NOT NULL,

    description VARCHAR(500),

    metadata JSON,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT uk_wallet_transaction_id
        UNIQUE (transaction_id),

    CONSTRAINT fk_wallet_transaction_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_wallet_transaction_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES wallets(id)
        ON DELETE CASCADE,

    INDEX idx_wallet_transaction_user_id (user_id),

    INDEX idx_wallet_transaction_wallet_id (wallet_id),

    INDEX idx_wallet_transaction_reference_id (reference_id),

    INDEX idx_wallet_transaction_created_at (created_at),

    INDEX idx_wallet_transaction_status (status)
);
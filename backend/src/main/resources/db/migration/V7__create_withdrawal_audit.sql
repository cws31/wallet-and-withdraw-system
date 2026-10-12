CREATE TABLE withdrawal_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    withdrawal_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    performed_by BIGINT,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_withdrawal_audit_withdrawal
        FOREIGN KEY (withdrawal_id)
        REFERENCES withdrawals(id),

    CONSTRAINT fk_withdrawal_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_withdrawal_audit_performed_by
        FOREIGN KEY (performed_by)
        REFERENCES users(id),

    INDEX idx_withdrawal_audit_withdrawal_id
        (withdrawal_id),

    INDEX idx_withdrawal_audit_user_id
        (user_id),

    INDEX idx_withdrawal_audit_created_at
        (created_at)
);
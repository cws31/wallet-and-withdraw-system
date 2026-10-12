CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,

    actor_id BIGINT,
    target_user_id BIGINT,
    target_type VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100),
    metadata TEXT,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_audit_log_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id),

    CONSTRAINT fk_audit_log_target_user
        FOREIGN KEY (target_user_id)
        REFERENCES users(id),

    INDEX idx_audit_log_actor_id
        (actor_id),

    INDEX idx_audit_log_target_user_id
        (target_user_id),

    INDEX idx_audit_log_target_type
        (target_type),

    INDEX idx_audit_log_action
        (action),

    INDEX idx_audit_log_reference_id
        (reference_id),

    INDEX idx_audit_log_created_at
        (created_at)
);
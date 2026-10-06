CREATE TABLE authentication_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,

    user_id BIGINT,
    email VARCHAR(255) NOT NULL,

    success BOOLEAN NOT NULL,

    created_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_authentication_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    INDEX idx_authentication_attempts_user_id (user_id),
    INDEX idx_authentication_attempts_email (email),
    INDEX idx_authentication_attempts_success_created_at (success, created_at),
    INDEX idx_authentication_attempts_user_success_created_at (
        user_id,
        success,
        created_at
    )
);

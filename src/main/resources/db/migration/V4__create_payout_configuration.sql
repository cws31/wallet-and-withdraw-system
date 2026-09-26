CREATE TABLE payout_methods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT uk_payout_method_code
        UNIQUE (code)
);

CREATE TABLE payout_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    method_id BIGINT NOT NULL,
    payout_amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(20) NOT NULL DEFAULT 'INR',
    currency_amount DECIMAL(19,4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_payout_option_method
        FOREIGN KEY (method_id)
        REFERENCES payout_methods(id)
        ON DELETE CASCADE,

    INDEX idx_payout_option_method_id (method_id),
    INDEX idx_payout_option_active (active)
);

INSERT INTO payout_methods (code, name, active)
VALUES
    ('UPI', 'UPI', TRUE),
    ('AMAZON_GIFT_CARD', 'Amazon Gift Card', TRUE),
    ('GOOGLE_PLAY_GIFT_CARD', 'Google Play Gift Card', TRUE),
    ('PAYPAL', 'PayPal', FALSE);


INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 10.0000, 'INR', 2400.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 25.0000, 'INR', 5800.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 50.0000, 'INR', 10000.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 100.0000, 'INR', 19500.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 150.0000, 'INR', 28500.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 300.0000, 'INR', 52500.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 500.0000, 'INR', 80500.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';

INSERT INTO payout_options
    (method_id, payout_amount, currency, currency_amount, active)
SELECT id, 1000.0000, 'INR', 150000.0000, TRUE
FROM payout_methods
WHERE code = 'UPI';
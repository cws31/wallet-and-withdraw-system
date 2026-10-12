ALTER TABLE fraud_risk_events
    ADD COLUMN explanation TEXT AFTER triggered_rules;
-- Anti-cheat enhancements for test assignments and attempts
ALTER TABLE test_assignments ADD COLUMN tab_switch_threshold INTEGER DEFAULT 0;
ALTER TABLE test_assignments ADD COLUMN tab_switch_action VARCHAR(20) DEFAULT 'WARN';

ALTER TABLE test_attempts ADD COLUMN ip_address VARCHAR(45);
ALTER TABLE test_attempts ADD COLUMN flagged BOOLEAN DEFAULT FALSE;
ALTER TABLE test_attempts ADD COLUMN flag_reason VARCHAR(255);

CREATE INDEX idx_attempt_flagged ON test_attempts(flagged) WHERE flagged = TRUE;

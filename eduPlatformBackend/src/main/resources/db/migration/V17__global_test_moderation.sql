-- V17: Global test moderation flow
-- Teacher can submit a test for global visibility.
-- Moderator approves/rejects it.
-- Approved global tests are visible to ALL roles.

ALTER TABLE test_history
    ADD COLUMN global_status       VARCHAR(30)  NOT NULL DEFAULT 'NONE',
    ADD COLUMN global_rejection_reason TEXT,
    ADD COLUMN global_submitted_at TIMESTAMP,
    ADD COLUMN global_reviewed_at  TIMESTAMP,
    ADD COLUMN global_reviewed_by  UUID;

CREATE INDEX idx_test_history_global_status ON test_history(global_status)
    WHERE deleted_at IS NULL;

-- Also add grade_level to test_history for student filtering
ALTER TABLE test_history
    ADD COLUMN grade_level INTEGER;

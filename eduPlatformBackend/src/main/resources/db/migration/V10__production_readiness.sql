-- V10: Production readiness improvements
-- Covers: optimistic locking, OAuth fields, full-text search, session cleanup

-- A1: Add optimistic locking version column to payments
ALTER TABLE payments ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- A1: Add optimistic locking to user_subscriptions
ALTER TABLE user_subscriptions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;

-- B1/B2: Ensure google_id and telegram_id columns exist on users (may already exist from JPA auto-DDL)
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_id VARCHAR(255);

-- Create unique indexes for OAuth IDs if they don't exist
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_google_id ON users(google_id) WHERE google_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_telegram_id ON users(telegram_id) WHERE telegram_id IS NOT NULL;

-- D1: Full-text search GIN index for questions
-- First ensure the tsvector column exists
ALTER TABLE questions ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_question_search_vector ON questions USING GIN(search_vector);

-- Create trigger to auto-update search_vector (question_text is JSONB with uz_latn, uz_cyrl, en, ru keys)
CREATE OR REPLACE FUNCTION update_question_search_vector() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('simple',
        COALESCE(NEW.question_text ->> 'uz_latn', '') || ' ' ||
        COALESCE(NEW.question_text ->> 'uz_cyrl', '') || ' ' ||
        COALESCE(NEW.question_text ->> 'en', '') || ' ' ||
        COALESCE(NEW.question_text ->> 'ru', '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_question_search_vector ON questions;
CREATE TRIGGER trg_question_search_vector
    BEFORE INSERT OR UPDATE OF question_text ON questions
    FOR EACH ROW EXECUTE FUNCTION update_question_search_vector();

-- Populate search_vector for existing rows
UPDATE questions SET search_vector = to_tsvector('simple',
    COALESCE(question_text ->> 'uz_latn', '') || ' ' ||
    COALESCE(question_text ->> 'uz_cyrl', '') || ' ' ||
    COALESCE(question_text ->> 'en', '') || ' ' ||
    COALESCE(question_text ->> 'ru', '')
)
WHERE search_vector IS NULL;

-- Session cleanup index for efficient expired session queries
CREATE INDEX IF NOT EXISTS idx_session_expires_active ON user_sessions(expires_at, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_session_user_active ON user_sessions(user_id, is_active) WHERE is_active = true;

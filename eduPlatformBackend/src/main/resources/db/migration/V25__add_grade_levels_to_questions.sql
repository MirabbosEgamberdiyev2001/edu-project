-- Add grade_levels (multi-select) to questions
-- Stored as JSONB array of integers e.g. [5, 6, 7]
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS grade_levels JSONB NOT NULL DEFAULT '[]';

CREATE INDEX IF NOT EXISTS idx_questions_grade_levels
    ON questions USING GIN (grade_levels);

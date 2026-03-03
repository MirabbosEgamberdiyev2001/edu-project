-- Feature: Grading strategy for SHORT_ANSWER and FILL_BLANK questions
-- Values: EXACT_MATCH | CONTAINS | MANUAL (default)
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS grading_strategy VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

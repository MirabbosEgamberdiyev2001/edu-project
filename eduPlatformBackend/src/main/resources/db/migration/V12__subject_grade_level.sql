-- Add grade_level column to subjects table
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS grade_level INTEGER;

-- Create index for grade_level filtering
CREATE INDEX IF NOT EXISTS idx_subjects_grade_level ON subjects (grade_level) WHERE deleted_at IS NULL;

-- Update the unique constraint to be grade-aware
DROP INDEX IF EXISTS uq_subjects_user_default_name;
CREATE UNIQUE INDEX uq_subjects_user_default_name_grade
ON subjects (user_id, LOWER(name ->> 'uz_latn'), COALESCE(grade_level, -1))
WHERE deleted_at IS NULL;

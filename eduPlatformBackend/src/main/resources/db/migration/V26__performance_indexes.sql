-- Performance indexes for frequently queried columns

-- Homework: teacher + status queries
CREATE INDEX IF NOT EXISTS idx_homeworks_teacher_status
    ON homeworks (teacher_id, status) WHERE deleted_at IS NULL;

-- Homework: group + status (student view)
CREATE INDEX IF NOT EXISTS idx_homeworks_group_status
    ON homeworks (group_id, status) WHERE deleted_at IS NULL;

-- Homework submissions: student's own submissions
CREATE INDEX IF NOT EXISTS idx_hw_submissions_student
    ON homework_submissions (student_id, submitted_at DESC);

-- In-app notifications: unread count query (already has is_read index, adding covering index)
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON in_app_notifications (user_id, is_read) WHERE is_read = FALSE;

-- Questions: composite filter index (subject + status via topic join is JPA Spec)
-- Partial index for ACTIVE questions (most commonly queried)
CREATE INDEX IF NOT EXISTS idx_questions_status_type
    ON questions (status, question_type) WHERE deleted_at IS NULL;

-- Test attempts: student history lookups
CREATE INDEX IF NOT EXISTS idx_test_attempts_student_date
    ON test_attempts (student_id, submitted_at DESC) WHERE deleted_at IS NULL;

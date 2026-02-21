-- ============================================
-- V5: Assessment Module Tables
-- TestAssignment, TestAttempt, Answer
-- ============================================

-- 1. Test Assignments (teacher assigns a generated test to students)
CREATE TABLE test_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- References
    test_history_id UUID NOT NULL REFERENCES test_history(id),
    teacher_id      UUID NOT NULL REFERENCES users(id),

    -- Info
    title       VARCHAR(255) NOT NULL,
    description TEXT,

    -- Scheduling
    start_time       TIMESTAMP,
    end_time         TIMESTAMP,
    duration_minutes INTEGER NOT NULL DEFAULT 45,

    -- Attempt settings
    max_attempts INTEGER NOT NULL DEFAULT 1,

    -- Result visibility
    show_results         BOOLEAN NOT NULL DEFAULT TRUE,
    show_correct_answers BOOLEAN NOT NULL DEFAULT FALSE,
    show_proofs          BOOLEAN NOT NULL DEFAULT FALSE,

    -- Shuffling
    shuffle_per_student BOOLEAN NOT NULL DEFAULT TRUE,

    -- Anti-cheat
    prevent_copy_paste BOOLEAN NOT NULL DEFAULT TRUE,
    prevent_tab_switch BOOLEAN NOT NULL DEFAULT FALSE,

    -- Access control
    access_code VARCHAR(20),

    -- Assigned students (JSONB array of UUIDs)
    assigned_student_ids JSONB,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- Audit
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT check_assignment_status CHECK (status IN (
        'DRAFT', 'SCHEDULED', 'ACTIVE', 'COMPLETED', 'CANCELLED'
    )),
    CONSTRAINT check_duration CHECK (duration_minutes > 0 AND duration_minutes <= 480),
    CONSTRAINT check_max_attempts CHECK (max_attempts > 0 AND max_attempts <= 10),
    CONSTRAINT check_time_range CHECK (end_time IS NULL OR start_time IS NULL OR end_time > start_time)
);

-- Indexes
CREATE INDEX idx_assignment_teacher ON test_assignments(teacher_id);
CREATE INDEX idx_assignment_test_history ON test_assignments(test_history_id);
CREATE INDEX idx_assignment_status ON test_assignments(status);
CREATE INDEX idx_assignment_start_time ON test_assignments(start_time);
CREATE INDEX idx_assignment_end_time ON test_assignments(end_time);

-- 2. Test Attempts (student's attempt at a test assignment)
CREATE TABLE test_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- References
    assignment_id UUID NOT NULL REFERENCES test_assignments(id) ON DELETE CASCADE,
    student_id    UUID NOT NULL REFERENCES users(id),

    -- Attempt tracking
    attempt_number INTEGER NOT NULL DEFAULT 1,
    variant_index  INTEGER,

    -- Timing
    started_at   TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP,

    -- Scoring
    raw_score  DECIMAL(7,2),
    max_score  DECIMAL(7,2),
    percentage DECIMAL(5,2),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',

    -- Anti-cheat
    tab_switch_count INTEGER DEFAULT 0,

    -- Audit
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_attempt_assignment_student_number UNIQUE (assignment_id, student_id, attempt_number),
    CONSTRAINT check_attempt_status CHECK (status IN (
        'IN_PROGRESS', 'SUBMITTED', 'AUTO_GRADED', 'NEEDS_REVIEW', 'GRADED'
    )),
    CONSTRAINT check_percentage CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100))
);

-- Indexes
CREATE INDEX idx_attempt_assignment ON test_attempts(assignment_id);
CREATE INDEX idx_attempt_student ON test_attempts(student_id);
CREATE INDEX idx_attempt_status ON test_attempts(status);

-- 3. Answers (individual question answers within an attempt)
CREATE TABLE answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- References
    attempt_id  UUID NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id),

    -- Position
    question_index INTEGER NOT NULL,

    -- Student's answer (JSONB - supports all question types)
    selected_answer JSONB,

    -- Grading
    is_correct    BOOLEAN,
    is_partial    BOOLEAN,
    earned_points DECIMAL(5,2),
    max_points    DECIMAL(5,2),

    -- Manual grading
    needs_manual_grading BOOLEAN DEFAULT FALSE,
    manual_score         DECIMAL(5,2),
    manual_feedback      TEXT,
    graded_by            UUID REFERENCES users(id),
    graded_at            TIMESTAMP,

    -- Metadata
    time_spent_seconds INTEGER,
    bookmarked         BOOLEAN NOT NULL DEFAULT FALSE,

    -- Audit
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uk_answer_attempt_question UNIQUE (attempt_id, question_id)
);

-- Indexes
CREATE INDEX idx_answer_attempt ON answers(attempt_id);
CREATE INDEX idx_answer_question ON answers(question_id);

-- Homework module: teacher creates assignments that students submit text/files for
CREATE TABLE homeworks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id          UUID NOT NULL,
    group_id            UUID,                               -- if null → all students of teacher
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    deadline            TIMESTAMP,
    allow_resubmission  BOOLEAN NOT NULL DEFAULT TRUE,
    file_required       BOOLEAN NOT NULL DEFAULT FALSE,     -- require student to upload a file
    max_file_size_mb    INTEGER NOT NULL DEFAULT 10,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CLOSED | DRAFT
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP
);

CREATE TABLE homework_submissions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id         UUID NOT NULL REFERENCES homeworks(id),
    student_id          UUID NOT NULL,
    text_answer         TEXT,
    file_url            VARCHAR(1000),
    file_name           VARCHAR(500),
    submitted_at        TIMESTAMP NOT NULL DEFAULT now(),
    grade               NUMERIC(5,2),                       -- 0.00 - 100.00 (percentage)
    teacher_comment     TEXT,
    graded_at           TIMESTAMP,
    graded_by           UUID,
    attempt_number      INTEGER NOT NULL DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'  -- SUBMITTED | GRADED | RETURNED
);

CREATE INDEX idx_homeworks_teacher    ON homeworks(teacher_id);
CREATE INDEX idx_homeworks_group      ON homeworks(group_id);
CREATE INDEX idx_hw_sub_homework      ON homework_submissions(homework_id);
CREATE INDEX idx_hw_sub_student       ON homework_submissions(student_id);
CREATE UNIQUE INDEX uk_hw_sub_latest ON homework_submissions(homework_id, student_id, attempt_number);

-- Export jobs table for async export processing
CREATE TABLE export_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID         NOT NULL,
    teacher_id      UUID         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    format          VARCHAR(10)  NOT NULL,
    locale          VARCHAR(10),
    file_name       VARCHAR(255),
    content_type    VARCHAR(100),
    file_data       BYTEA,
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    expires_at      TIMESTAMP
);

CREATE INDEX idx_export_job_teacher    ON export_jobs (teacher_id);
CREATE INDEX idx_export_job_status     ON export_jobs (status);
CREATE INDEX idx_export_job_expires_at ON export_jobs (expires_at);

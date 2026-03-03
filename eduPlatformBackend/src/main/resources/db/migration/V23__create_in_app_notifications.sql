-- In-app notifications for teachers and other users
CREATE TABLE in_app_notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,                      -- recipient
    type            VARCHAR(50) NOT NULL,               -- e.g. ATTEMPT_SUBMITTED, NEEDS_REVIEW
    title           VARCHAR(255) NOT NULL,
    body            TEXT,
    reference_id    UUID,                               -- related entity id (attemptId, questionId, etc.)
    reference_type  VARCHAR(50),                        -- ATTEMPT | QUESTION | ASSIGNMENT
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_in_app_notif_user_unread ON in_app_notifications(user_id, is_read);
CREATE INDEX idx_in_app_notif_created ON in_app_notifications(created_at DESC);

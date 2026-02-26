CREATE TABLE assignment_promo_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID NOT NULL REFERENCES test_assignments(id) ON DELETE CASCADE,
    code            VARCHAR(8) NOT NULL,
    max_uses        INTEGER,
    current_uses    INTEGER NOT NULL DEFAULT 0,
    expires_at      TIMESTAMP,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    CONSTRAINT uk_promo_code UNIQUE (code),
    CONSTRAINT chk_max_uses CHECK (max_uses IS NULL OR max_uses > 0)
);

CREATE INDEX idx_promo_code_assignment ON assignment_promo_codes(assignment_id);
CREATE INDEX idx_promo_code_code ON assignment_promo_codes(code);

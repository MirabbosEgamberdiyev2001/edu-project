-- ============================================
-- V21: Fix parent_children table constraints
--
-- Bug 1: check_different_users prevents the service from creating
--        a placeholder record with parentId = childId = studentId
--        (the parent slot is filled when the parent claims the code).
--
-- Bug 2: check_pairing_status does not include EXPIRED, but the
--        service sets status = EXPIRED when cleaning up stale records.
-- ============================================

-- Drop the constraint that blocks the placeholder pairing record
ALTER TABLE parent_children
    DROP CONSTRAINT IF EXISTS check_different_users;

-- Widen the status check to include EXPIRED
ALTER TABLE parent_children
    DROP CONSTRAINT IF EXISTS check_pairing_status;

ALTER TABLE parent_children
    ADD CONSTRAINT check_pairing_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED', 'EXPIRED'));

-- Add group_id column to test_assignments for tracking which group the assignment was assigned to
ALTER TABLE test_assignments ADD COLUMN IF NOT EXISTS group_id UUID;

-- Add index for group-based queries
CREATE INDEX IF NOT EXISTS idx_assignment_group ON test_assignments(group_id);

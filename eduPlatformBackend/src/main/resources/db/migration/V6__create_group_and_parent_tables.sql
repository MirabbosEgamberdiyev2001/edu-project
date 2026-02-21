-- ============================================
-- V6: Groups Module + Parent-Child Pairing
-- StudentGroup, GroupMember, ParentChild
-- ============================================

-- 1. Student Groups
CREATE TABLE student_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    teacher_id UUID NOT NULL REFERENCES users(id),
    subject_id UUID REFERENCES subjects(id),

    name       VARCHAR(255) NOT NULL,
    description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,

    CONSTRAINT check_group_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_group_teacher ON student_groups(teacher_id);
CREATE INDEX idx_group_subject ON student_groups(subject_id);
CREATE INDEX idx_group_status ON student_groups(status);

-- 2. Group Members (student-group many-to-many)
CREATE TABLE group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    group_id   UUID NOT NULL REFERENCES student_groups(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id),

    joined_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uk_group_student UNIQUE (group_id, student_id)
);

CREATE INDEX idx_gm_group ON group_members(group_id);
CREATE INDEX idx_gm_student ON group_members(student_id);

-- 3. Parent-Child Pairing
CREATE TABLE parent_children (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    parent_id UUID NOT NULL REFERENCES users(id),
    child_id  UUID NOT NULL REFERENCES users(id),

    pairing_code            VARCHAR(8) UNIQUE,
    pairing_code_expires_at TIMESTAMP,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    paired_at  TIMESTAMP DEFAULT NOW(),
    revoked_at TIMESTAMP,

    CONSTRAINT uk_parent_child UNIQUE (parent_id, child_id),
    CONSTRAINT check_pairing_status CHECK (status IN ('PENDING', 'ACTIVE', 'REVOKED')),
    CONSTRAINT check_different_users CHECK (parent_id != child_id)
);

CREATE INDEX idx_pc_parent ON parent_children(parent_id);
CREATE INDEX idx_pc_child ON parent_children(child_id);

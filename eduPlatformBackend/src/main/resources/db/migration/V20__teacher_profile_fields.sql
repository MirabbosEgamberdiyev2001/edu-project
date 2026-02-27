-- V20: Add teacher profile fields (bio, workplace, subject_id)
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS workplace VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS subject_id UUID REFERENCES subjects(id);

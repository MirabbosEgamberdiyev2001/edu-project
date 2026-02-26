-- Add multilingual title/description support to test_assignments
ALTER TABLE test_assignments ADD COLUMN title_translations jsonb;
ALTER TABLE test_assignments ADD COLUMN description_translations jsonb;

-- Migrate existing values to translations map (uz_latn as default)
UPDATE test_assignments
SET title_translations = jsonb_build_object('uz_latn', title)
WHERE title IS NOT NULL AND title != '';

UPDATE test_assignments
SET description_translations = jsonb_build_object('uz_latn', description)
WHERE description IS NOT NULL AND description != '';

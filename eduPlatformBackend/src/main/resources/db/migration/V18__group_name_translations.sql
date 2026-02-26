-- Add multilingual name/description support to student_groups
ALTER TABLE student_groups ADD COLUMN name_translations jsonb;
ALTER TABLE student_groups ADD COLUMN description_translations jsonb;

-- Migrate existing values to translations map (uz_latn as default)
UPDATE student_groups
SET name_translations = jsonb_build_object('uz_latn', name)
WHERE name IS NOT NULL AND name != '';

UPDATE student_groups
SET description_translations = jsonb_build_object('uz_latn', description)
WHERE description IS NOT NULL AND description != '';

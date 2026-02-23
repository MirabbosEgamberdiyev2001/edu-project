-- Add multilingual title support to test_history
ALTER TABLE test_history ADD COLUMN title_translations jsonb;

-- Migrate existing title values to translations map (uz_latn as default)
UPDATE test_history
SET title_translations = jsonb_build_object('uz_latn', title)
WHERE title IS NOT NULL AND title != '';

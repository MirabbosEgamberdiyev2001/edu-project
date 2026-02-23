CREATE UNIQUE INDEX uq_subjects_user_default_name
ON subjects (user_id, LOWER(name ->> 'uz_latn'))
WHERE deleted_at IS NULL;

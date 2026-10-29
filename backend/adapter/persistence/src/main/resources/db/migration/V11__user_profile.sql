ALTER TABLE users
    ADD COLUMN first_name TEXT NULL,
    ADD COLUMN last_name  TEXT NULL,
    ADD CONSTRAINT ck_users_full_name_together CHECK (
        (first_name IS NULL AND last_name IS NULL)
        OR (length(trim(first_name)) > 0 AND length(trim(last_name)) > 0)
    );

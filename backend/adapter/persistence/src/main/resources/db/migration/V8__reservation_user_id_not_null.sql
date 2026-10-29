DELETE FROM reservation WHERE user_id IS NULL;

ALTER TABLE reservation
    DROP CONSTRAINT fk_reservation_user;

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE reservation
    ALTER COLUMN user_id SET NOT NULL;

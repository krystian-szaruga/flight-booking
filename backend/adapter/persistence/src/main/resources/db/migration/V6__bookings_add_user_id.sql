ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_reservation_user ON reservation (user_id);

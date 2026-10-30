ALTER TABLE reservation_seat
    ADD COLUMN passport_number_ciphertext TEXT,
    ADD COLUMN passport_number_last4 TEXT,
    ADD CONSTRAINT ck_reservation_seat_passport_pair CHECK (
        (passport_number_ciphertext IS NULL) = (passport_number_last4 IS NULL)
    );

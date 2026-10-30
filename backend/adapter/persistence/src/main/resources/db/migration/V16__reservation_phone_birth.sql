ALTER TABLE reservation_seat
    ADD COLUMN phone_number_ciphertext TEXT,
    ADD COLUMN phone_number_last4 TEXT,
    ADD COLUMN date_of_birth DATE,
    ADD CONSTRAINT ck_reservation_seat_phone_pair CHECK (
        (phone_number_ciphertext IS NULL) = (phone_number_last4 IS NULL)
    );

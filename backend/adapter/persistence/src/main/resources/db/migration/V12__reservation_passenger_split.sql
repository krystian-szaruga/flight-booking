UPDATE flight_seat
   SET status = 'AVAILABLE',
       hold_expires_at = NULL,
       reservation_id = NULL
 WHERE reservation_id IS NOT NULL;

DELETE FROM reservation;

ALTER TABLE reservation_seat
    DROP CONSTRAINT ck_reservation_seat_passenger_not_blank,
    DROP COLUMN passenger_name,
    ADD COLUMN first_name TEXT NOT NULL,
    ADD COLUMN last_name  TEXT NOT NULL,
    ADD CONSTRAINT ck_reservation_seat_first_name_not_blank CHECK (length(trim(first_name)) > 0),
    ADD CONSTRAINT ck_reservation_seat_last_name_not_blank CHECK (length(trim(last_name)) > 0);

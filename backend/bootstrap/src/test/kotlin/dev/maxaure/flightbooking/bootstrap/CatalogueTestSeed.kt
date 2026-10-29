package dev.maxaure.flightbooking.bootstrap

import org.springframework.jdbc.core.JdbcTemplate

internal class CatalogueTestSeed(private val jdbc: JdbcTemplate) {

    fun clearReservations() {
        jdbc.update("UPDATE flight_seat SET status = 'AVAILABLE', hold_expires_at = NULL, reservation_id = NULL")
        jdbc.update("DELETE FROM reservation_seat")
        jdbc.update("DELETE FROM reservation")
    }

    fun clearUsers() {
        jdbc.update("DELETE FROM pending_registration")
        jdbc.update("DELETE FROM users")
    }

    fun clearCatalogue() {
        jdbc.update("DELETE FROM flight_seat")
        jdbc.update("DELETE FROM flight")
        jdbc.update("DELETE FROM airport")
    }

    fun seedCatalogue() {
        jdbc.update(
            """
            INSERT INTO airport (id, iata_code, name, city, country_code)
            VALUES (?::uuid, 'WAW', 'Warsaw Chopin', 'Warsaw', 'PL'),
                   (?::uuid, 'LIS', 'Lisbon', 'Lisbon', 'PT')
            """,
            ORIGIN_ID,
            DESTINATION_ID,
        )
        jdbc.update(
            """
            INSERT INTO flight (id, flight_number, origin_airport_id, destination_airport_id, departure_at, arrival_at, aircraft)
            VALUES (?::uuid, 'LO381', ?::uuid, ?::uuid, '2026-11-10T08:15:00Z', '2026-11-10T12:35:00Z', 'A320neo')
            """,
            FLIGHT_ID,
            ORIGIN_ID,
            DESTINATION_ID,
        )
        jdbc.update(
            """
            INSERT INTO fare_class (id, flight_id, cabin, base_price_amount, currency)
            VALUES (?::uuid, ?::uuid, 'ECONOMY', 399.00, 'PLN')
            """,
            ECONOMY_ID,
            FLIGHT_ID,
        )
        SEATS.forEach { seat ->
            jdbc.update(
                """
                INSERT INTO flight_seat (id, flight_id, fare_class_id, seat_number, status)
                VALUES (gen_random_uuid(), ?::uuid, ?::uuid, ?, 'AVAILABLE')
                """,
                FLIGHT_ID,
                ECONOMY_ID,
                seat,
            )
        }
    }

    companion object {
        const val FLIGHT_ID = "11111111-1111-1111-1111-111111111111"
        private const val ORIGIN_ID = "22222222-2222-2222-2222-222222222222"
        private const val DESTINATION_ID = "33333333-3333-3333-3333-333333333333"
        private const val ECONOMY_ID = "44444444-4444-4444-4444-444444444444"

        val SEATS = listOf("12A", "12B", "12C", "12D", "12E", "12F", "13A", "13B")
    }
}

package dev.maxaure.flightbooking.bootstrap

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@ApplyExtension(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReservationItSmokeTest(
    @Autowired private val jdbc: JdbcTemplate,
    @LocalServerPort private val port: Int,
) : StringSpec({

    val seed = CatalogueTestSeed(jdbc)

    beforeTest {
        seed.clearReservations()
        seed.clearUsers()
        seed.clearCatalogue()
        seed.seedCatalogue()
    }

    "when two users hold seats then each lists only their own reservations" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val bolek = clientFor(port).also { register(it, "bolek@example.com") }
        val flightId = seededFlightId(jdbc)

        hold(anna, "anna@example.com", flightId, "12A")
        hold(bolek, "bolek@example.com", flightId, "12B")

        val annaIds = reservationIdsOf(anna, "anna@example.com")
        val bolekIds = reservationIdsOf(bolek, "bolek@example.com")

        annaIds.size shouldBeExactly 1
        bolekIds.size shouldBeExactly 1
        annaIds.first() shouldNotBe bolekIds.first()
    }

    "when a user has no reservations then their listing is empty" {
        val celina = clientFor(port).also { register(it, "celina@example.com") }

        reservationIdsOf(celina, "celina@example.com").shouldBeEmpty()
    }

    "when user A cancels user B's reservation then it is not found and B keeps it" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val bolek = clientFor(port).also { register(it, "bolek@example.com") }
        val flightId = seededFlightId(jdbc)

        hold(bolek, "bolek@example.com", flightId, "12A")
        val bolekReservation = reservationIdsOf(bolek, "bolek@example.com").single()

        val status = anna.delete()
            .uri("/api/v1/reservations/$bolekReservation")
            .header(HttpHeaders.COOKIE, sessionCookie(anna, "anna@example.com"))
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 404
        reservationIdsOf(bolek, "bolek@example.com") shouldContainExactly listOf(bolekReservation)
    }

    "when several users hold seats on one flight then the listing counts match the holds" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val bolek = clientFor(port).also { register(it, "bolek@example.com") }
        val flightId = seededFlightId(jdbc)

        hold(bolek, "bolek@example.com", flightId, "12A")
        hold(bolek, "bolek@example.com", flightId, "12B")
        hold(anna, "anna@example.com", flightId, "13A")

        reservationIdsOf(anna, "anna@example.com").size shouldBeExactly 1
        reservationIdsOf(bolek, "bolek@example.com").size shouldBeExactly 2
    }
}) {

    companion object {
        @JvmStatic
        val postgres: PostgreSQLContainer<Nothing> =
            PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16-alpine"))
                .apply { start() }

        private fun clientFor(port: Int): RestClient = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .defaultStatusHandler({ true }, { _, _ -> })
            .build()

        private fun register(client: RestClient, email: String) {
            client.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"email":"$email","password":"s3cret123"}""")
                .retrieve()
                .toBodilessEntity()
        }

        private fun sessionCookie(client: RestClient, email: String): String {
            val response = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"email":"$email","password":"s3cret123"}""")
                .retrieve()
                .toBodilessEntity()
            return response.headers.getFirst(HttpHeaders.SET_COOKIE)!!.substringBefore(';')
        }

        private fun hold(client: RestClient, email: String, flightId: String, seat: String) {
            client.post()
                .uri("/api/v1/reservations/flights/$flightId")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, sessionCookie(client, email))
                .body("""{"seats":[{"seat":"$seat","passengerName":"Ada Lovelace"}]}""")
                .retrieve()
                .toBodilessEntity()
        }

        private fun reservationIdsOf(client: RestClient, email: String): List<String> =
            client.get()
                .uri("/api/v1/reservations")
                .header(HttpHeaders.COOKIE, sessionCookie(client, email))
                .retrieve()
                .toEntity(String::class.java)
                .body!!
                .let { body -> REGEX_RESERVATION_ID.findAll(body).map { it.groupValues[1] }.toList() }

        private val REGEX_RESERVATION_ID = """"reservationId"\s*:\s*"([0-9a-fA-F-]{36})"""".toRegex()

        private fun seededFlightId(jdbc: JdbcTemplate): String = CatalogueTestSeed.FLIGHT_ID

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}

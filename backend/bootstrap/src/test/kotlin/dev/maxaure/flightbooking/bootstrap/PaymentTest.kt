package dev.maxaure.flightbooking.bootstrap

import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration

@ApplyExtension(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MutableClockConfig::class, MailOutboxConfig::class)
class PaymentTest(
    @Autowired private val jdbc: JdbcTemplate,
    @Autowired private val clock: MutableClock,
    @LocalServerPort private val port: Int,
) : StringSpec({

    val seed = CatalogueTestSeed(jdbc)

    beforeTest {
        clock.reset()
        seed.clearReservations()
        seed.clearUsers()
        seed.clearCatalogue()
        seed.seedCatalogue()
    }

    "when a hold is paid with the approving card then the reservation is confirmed and its seats are sold" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val reservationId = hold(anna, "anna@example.com", "12A")

        val (status, body) = pay(anna, "anna@example.com", reservationId, "key-1", APPROVED_CARD)

        status shouldBeExactly 201
        body shouldContain "\"status\":\"APPROVED\""
        body shouldContain "\"cardLast4\":\"4242\""
        reservationStatus(jdbc, reservationId) shouldBe "CONFIRMED"
        seatStatus(jdbc, "12A") shouldBe "SOLD"
        paymentCount(jdbc) shouldBe 1
        paymentAmount(jdbc, "key-1") shouldBe BigDecimal("399.00")
        paymentCurrency(jdbc, "key-1") shouldBe "PLN"
    }

    "when the card is declined then the payment is recorded and the hold stays pending" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val reservationId = hold(anna, "anna@example.com", "12A")

        val (status, body) = pay(anna, "anna@example.com", reservationId, "key-2", DECLINED_CARD)

        status shouldBeExactly 402
        body shouldContain "\"status\":\"DECLINED\""
        body shouldContain "Card declined by issuer"
        reservationStatus(jdbc, reservationId) shouldBe "PENDING"
        seatStatus(jdbc, "12A") shouldBe "HELD"
        paymentCount(jdbc) shouldBe 1
        listReservations(anna, "anna@example.com") shouldContain "\"paid\":null"
    }

    "when a paid reservation is listed then it reports both the price and the amount paid" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val reservationId = hold(anna, "anna@example.com", "12A")
        pay(anna, "anna@example.com", reservationId, "key-5", APPROVED_CARD).first shouldBeExactly 201

        val listed = listReservations(anna, "anna@example.com")

        listed shouldContain "\"price\":{\"amount\":399"
        listed shouldContain "\"paid\":{\"amount\":399"
        listed shouldContain "\"currency\":\"PLN\""
        listed shouldContain "\"cardLast4\":\"4242\""
    }

    "when the same idempotency key is retried then the same payment comes back and nothing is charged twice" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val reservationId = hold(anna, "anna@example.com", "12A")

        val first = pay(anna, "anna@example.com", reservationId, "replay-me", APPROVED_CARD)
        val replay = pay(anna, "anna@example.com", reservationId, "replay-me", APPROVED_CARD)

        first.first shouldBeExactly 201
        replay.first shouldBeExactly 201
        paymentId(replay.second) shouldBe paymentId(first.second)
        paymentCount(jdbc) shouldBe 1
    }

    "when an idempotency key is reused for another reservation then it is rejected and that hold survives" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val paid = hold(anna, "anna@example.com", "12A")
        val other = hold(anna, "anna@example.com", "12B")
        pay(anna, "anna@example.com", paid, "shared-key", APPROVED_CARD)

        val (status, _) = pay(anna, "anna@example.com", other, "shared-key", APPROVED_CARD)

        status shouldBeExactly 409
        reservationStatus(jdbc, other) shouldBe "PENDING"
        seatStatus(jdbc, "12B") shouldBe "HELD"
        paymentCount(jdbc) shouldBe 1
    }

    "when another user pays someone else's reservation then it is not found and nothing is charged" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val bolek = clientFor(port).also { register(it, "bolek@example.com") }
        val reservationId = hold(anna, "anna@example.com", "12A")

        val (status, _) = pay(bolek, "bolek@example.com", reservationId, "key-3", APPROVED_CARD)

        status shouldBeExactly 404
        reservationStatus(jdbc, reservationId) shouldBe "PENDING"
        paymentCount(jdbc) shouldBe 0
    }

    "when the hold has expired then the payment is refused and nothing is charged" {
        val anna = clientFor(port).also { register(it, "anna@example.com") }
        val reservationId = hold(anna, "anna@example.com", "12A")
        clock.advance(Duration.ofMinutes(16))

        val (status, _) = pay(anna, "anna@example.com", reservationId, "key-4", APPROVED_CARD)

        status shouldBeExactly 409
        reservationStatus(jdbc, reservationId) shouldBe "PENDING"
        paymentCount(jdbc) shouldBe 0
    }
}) {

    companion object {
        private const val APPROVED_CARD = "4242424242424242"
        private const val DECLINED_CARD = "4000000000000002"
        private const val CARD_HOLDER = "Ada Lovelace"

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

            client.post()
                .uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"email":"$email","code":"${MailOutbox.lastCodeFor(email)}"}""")
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

        private fun hold(client: RestClient, email: String, seat: String): String {
            val (status, body) = client.post()
                .uri("/api/v1/reservations/flights/${CatalogueTestSeed.FLIGHT_ID}")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, sessionCookie(client, email))
                .body("""{"seats":[{"seat":"$seat","passengerType":"OTHER","firstName":"Ada","lastName":"Lovelace","passportNumber":"X1234567","phoneNumber":"+48600123456","dateOfBirth":"1990-01-01"}]}""")
                .exchange { _, res -> res.statusCode.value() to String(res.body.readAllBytes()) }

            check(status in 200..299) { "hold $seat for $email failed: HTTP $status $body" }
            return RESERVATION_ID.find(body)!!.groupValues[1]
        }

        private fun pay(
            client: RestClient,
            email: String,
            reservationId: String,
            idempotencyKey: String,
            card: String,
        ): Pair<Int, String> =
            client.post()
                .uri("/api/v1/reservations/$reservationId/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, sessionCookie(client, email))
                .body(
                    """{"idempotencyKey":"$idempotencyKey","cardNumber":"$card","cardHolder":"$CARD_HOLDER"}""",
                )
                .exchange { _, res -> res.statusCode.value() to String(res.body.readAllBytes()) }

        private fun paymentId(body: String): String = PAYMENT_ID.find(body)!!.groupValues[1]

        private fun listReservations(client: RestClient, email: String): String =
            client.get()
                .uri("/api/v1/reservations")
                .header(HttpHeaders.COOKIE, sessionCookie(client, email))
                .retrieve()
                .toEntity<String>()
                .body!!

        private val RESERVATION_ID = """"reservationId"\s*:\s*"([0-9a-fA-F-]{36})"""".toRegex()
        private val PAYMENT_ID = """"paymentId"\s*:\s*"([0-9a-fA-F-]{36})"""".toRegex()

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}

private fun reservationStatus(jdbc: JdbcTemplate, reservationId: String): String =
    jdbc.queryForObject<String>(
        "SELECT status FROM reservation WHERE id = ?::uuid",
        reservationId,
    )!!

private fun seatStatus(jdbc: JdbcTemplate, seat: String): String =
    jdbc.queryForObject<String>(
        "SELECT status FROM flight_seat WHERE flight_id = ?::uuid AND seat_number = ?",
        CatalogueTestSeed.FLIGHT_ID,
        seat,
    )!!

private fun paymentCount(jdbc: JdbcTemplate): Long =
    jdbc.queryForObject<Long>("SELECT count(*) FROM payment")!!

private fun paymentAmount(jdbc: JdbcTemplate, idempotencyKey: String): BigDecimal =
    jdbc.queryForObject(
        "SELECT amount FROM payment WHERE idempotency_key = ?",
        BigDecimal::class.java,
        idempotencyKey,
    )!!

private fun paymentCurrency(jdbc: JdbcTemplate, idempotencyKey: String): String =
    jdbc.queryForObject<String>(
        "SELECT currency FROM payment WHERE idempotency_key = ?",
        idempotencyKey,
    )!!

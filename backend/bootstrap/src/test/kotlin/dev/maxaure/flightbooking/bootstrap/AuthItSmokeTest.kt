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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@ApplyExtension(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MutableClockConfig::class, MailOutboxConfig::class)
class AuthItSmokeTest(
    @Autowired private val jdbc: JdbcTemplate,
    @Autowired private val clock: MutableClock,
    @LocalServerPort private val port: Int,
) : StringSpec({

    beforeTest {
        clock.reset()
        CatalogueTestSeed(jdbc).clearReservations()
        jdbc.update("DELETE FROM pending_registration")
        jdbc.update("DELETE FROM users")
    }

    "when a new user registers then only a pending row is stored and no session cookie is issued" {
        val client = clientFor(port)
        val response = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerBody("anna@example.com"))
            .retrieve()
            .toBodilessEntity()

        response.statusCode.value() shouldBeExactly 202
        response.headers.getFirst(HttpHeaders.SET_COOKIE) shouldBe null
        jdbc.queryForObject<Int>("SELECT count(*) FROM users WHERE email = ?", "anna@example.com")!! shouldBeExactly 0
        jdbc.queryForObject<Int>(
            "SELECT count(*) FROM pending_registration WHERE email = ?",
            "anna@example.com",
        )!! shouldBeExactly 1
    }

    "when the correct code is submitted then the account is created verified and a session cookie is issued" {
        val client = clientFor(port)
        register(client, "verify@example.com")

        val response = client.post()
            .uri("/api/auth/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(verifyBody("verify@example.com", currentCode("verify@example.com")))
            .retrieve()
            .toBodilessEntity()

        response.statusCode.value() shouldBeExactly 200
        response.headers.getFirst(HttpHeaders.SET_COOKIE) shouldContain "access_token="
        jdbc.queryForObject<Int>(
            "SELECT count(*) FROM users WHERE email = ? AND email_verified_at IS NOT NULL",
            "verify@example.com",
        )!! shouldBeExactly 1
        jdbc.queryForObject<Int>(
            "SELECT count(*) FROM pending_registration WHERE email = ?",
            "verify@example.com",
        )!! shouldBeExactly 0
    }

    "when a wrong code is submitted then the generic failure is returned" {
        val client = clientFor(port)
        register(client, "wrongcode@example.com")

        val body = client.post()
            .uri("/api/auth/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(verifyBody("wrongcode@example.com", "000000"))
            .exchange { _, res -> res.statusCode.value() to res.body.readAllBytes().decodeToString() }

        body.first shouldBeExactly 400
        body.second shouldContain "invalid_or_expired_code"
    }

    "when an unknown email is verified then the body is identical to a wrong code" {
        val client = clientFor(port)
        register(client, "known@example.com")

        val unknown = client.post()
            .uri("/api/auth/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(verifyBody("ghost@example.com", "123456"))
            .exchange { _, res -> res.statusCode.value() to res.body.readAllBytes().decodeToString() }
        val wrong = client.post()
            .uri("/api/auth/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(verifyBody("known@example.com", "000000"))
            .exchange { _, res -> res.statusCode.value() to res.body.readAllBytes().decodeToString() }

        unknown.first shouldBeExactly wrong.first
        unknown.second shouldBe wrong.second
    }

    "when a registration is unfinished then holding seats is impossible because login is refused" {
        val client = clientFor(port)
        val seed = CatalogueTestSeed(jdbc)
        seed.clearReservations()
        seed.clearCatalogue()
        seed.seedCatalogue()
        register(client, "unverified@example.com")

        val login = client.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"unverified@example.com","password":"s3cret123"}""")
            .exchange { _, res -> res }

        login.statusCode.value() shouldBeExactly 401
        login.headers.getFirst(HttpHeaders.SET_COOKIE) shouldBe null
        jdbc.queryForObject<Int>("SELECT count(*) FROM users")!! shouldBeExactly 0
    }

    "when a verified user holds seats then the hold succeeds" {
        val client = clientFor(port)
        val seed = CatalogueTestSeed(jdbc)
        seed.clearReservations()
        seed.clearCatalogue()
        seed.seedCatalogue()
        registerAndVerify(client, "verified@example.com")

        val status = client.post()
            .uri("/api/v1/reservations/flights/${CatalogueTestSeed.FLIGHT_ID}")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.COOKIE, login(client, "verified@example.com", "s3cret123").cookie)
            .body("""{"seats":[{"seat":"12A","passengerName":"Ada Lovelace"}]}""")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 201
    }

    "when resend is requested inside the cooldown then it is accepted but the code is unchanged" {
        val client = clientFor(port)
        register(client, "cooldown@example.com")
        val before = currentCode("cooldown@example.com")

        val status = client.post()
            .uri("/api/auth/verify/resend")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"cooldown@example.com"}""")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 429
        currentCode("cooldown@example.com") shouldBe before
    }

    "when the code has expired then verification fails and resend is silent but register issues a new code" {
        val client = clientFor(port)
        register(client, "expired@example.com")
        val oldCode = currentCode("expired@example.com")
        clock.advance(Duration.ofMinutes(16))

        val stale = client.post()
            .uri("/api/auth/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(verifyBody("expired@example.com", oldCode))
            .exchange { _, res -> res.statusCode.value() }

        stale shouldBeExactly 400
        MailOutbox.sentCountFor("expired@example.com") shouldBeExactly 1

        register(client, "expired@example.com")
        MailOutbox.sentCountFor("expired@example.com") shouldBeExactly 2
        verify(client, "expired@example.com")
        login(client, "expired@example.com", "s3cret123")
    }

    "when the same email registers twice then the second attempt is accepted quietly with a new code" {
        val client = clientFor(port)
        register(client, "dup@example.com")
        val mailsBefore = MailOutbox.sentCountFor("dup@example.com")
        clock.advance(Duration.ofMinutes(1))

        val second = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerBody("DUP@example.com"))
            .exchange { _, res -> res.statusCode.value() to res.body.readAllBytes().decodeToString() }

        second.first shouldBeExactly 202
        second.second shouldContain "CODE_SENT"
        MailOutbox.sentCountFor("dup@example.com") shouldBeExactly mailsBefore + 1
        jdbc.queryForObject<Int>("SELECT count(*) FROM pending_registration")!! shouldBeExactly 1
    }

    "when a registered account registers again then the answer is identical but nothing is mailed" {
        val client = clientFor(port)
        register(client, "taken@example.com")
        verify(client, "taken@example.com")
        val mailsBefore = MailOutbox.sentCountFor("taken@example.com")

        val again = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerBody("taken@example.com"))
            .exchange { _, res -> res.statusCode.value() to res.body.readAllBytes().decodeToString() }

        again.first shouldBeExactly 202
        again.second shouldContain "EMAIL_VERIFIED"
        MailOutbox.sentCountFor("taken@example.com") shouldBeExactly mailsBefore
        jdbc.queryForObject<Int>("SELECT count(*) FROM users")!! shouldBeExactly 1
        jdbc.queryForObject<Int>("SELECT count(*) FROM pending_registration")!! shouldBeExactly 0
    }

    "when a pending registration exists then login is refused and the code can still be used" {
        val client = clientFor(port)
        register(client, "pending@example.com")

        val loginStatus = client.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"pending@example.com","password":"s3cret123"}""")
            .exchange { _, res -> res.statusCode.value() }

        loginStatus shouldBeExactly 401
        verify(client, "pending@example.com")
        login(client, "pending@example.com", "s3cret123")
    }

    "when the email is malformed then registration is rejected" {
        val client = clientFor(port)
        val status = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerBody("not-an-email"))
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 400
    }

    "when valid credentials are given then login issues a cookie and me identifies the user" {
        val client = clientFor(port)
        registerAndVerify(client, "login@example.com")
        val session = login(client, "login@example.com", "s3cret123")

        val me = client.get()
            .uri("/api/auth/me")
            .header(HttpHeaders.COOKIE, session.cookie)
            .retrieve()
            .toBodilessEntity()

        me.statusCode.value() shouldBeExactly 200
        me.headers.contentType!!.isCompatibleWith(MediaType.APPLICATION_JSON) shouldBe true
    }

    "when the password is wrong then login is rejected" {
        val client = clientFor(port)
        registerAndVerify(client, "wrong@example.com")

        val status = client.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"wrong@example.com","password":"nope-nope"}""")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 401
    }

    "when no cookie is sent then the me endpoint requires authentication" {
        val client = clientFor(port)
        val status = client.get()
            .uri("/api/auth/me")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 401
    }

    "when a user logs out then the old cookie no longer identifies them" {
        val client = clientFor(port)
        registerAndVerify(client, "out@example.com")
        val session = login(client, "out@example.com", "s3cret123")

        client.post()
            .uri("/api/auth/logout")
            .header(HttpHeaders.COOKIE, session.cookie)
            .retrieve()
            .toBodilessEntity()

        val status = client.get()
            .uri("/api/auth/me")
            .header(HttpHeaders.COOKIE, session.cookie)
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 401
    }
    "when the email is not registered then login is rejected" {
        val client = clientFor(port)

        val status = client.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"nobody@example.com","password":"s3cret123"}""")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 401
    }

    "when the email is registered in a different letter case then login succeeds" {
        val client = clientFor(port)
        registerAndVerify(client, "case@example.com")

        val status = client.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"CASE@Example.com","password":"s3cret123"}""")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 200
    }

    "when the password is too short then registration is rejected" {
        val client = clientFor(port)

        val status = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"email":"short@example.com","password":"s3cret"}""")
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 400
    }

    "when the cookie is tampered with then the me endpoint refuses it" {
        val client = clientFor(port)
        registerAndVerify(client, "tamper@example.com")
        val session = login(client, "tamper@example.com", "s3cret123")
        val tampered = session.cookie.dropLast(4) + "AAAA"

        val status = client.get()
            .uri("/api/auth/me")
            .header(HttpHeaders.COOKIE, tampered)
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 401
    }

    "when a user logs out then a fresh login still works" {
        val client = clientFor(port)
        registerAndVerify(client, "again@example.com")
        val first = login(client, "again@example.com", "s3cret123")

        client.post()
            .uri("/api/auth/logout")
            .header(HttpHeaders.COOKIE, first.cookie)
            .retrieve()
            .toBodilessEntity()
        val second = login(client, "again@example.com", "s3cret123")

        val status = client.get()
            .uri("/api/auth/me")
            .header(HttpHeaders.COOKIE, second.cookie)
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 200
    }
    "when the token is older than its ttl then it is rejected" {
        val client = clientFor(port)
        registerAndVerify(client, "expiry@example.com")
        val session = login(client, "expiry@example.com", "s3cret123")
        clock.advance(Duration.ofMinutes(61))

        val status = client.get()
            .uri("/api/auth/me")
            .header(HttpHeaders.COOKIE, session.cookie)
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 401
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

        private fun registerBody(email: String) = """{"email":"$email","password":"s3cret123"}"""

        private fun verifyBody(email: String, code: String) = """{"email":"$email","code":"$code"}"""

        private fun currentCode(email: String): String =
            MailOutbox.lastCodeFor(email)

        private fun register(client: RestClient, email: String) {
            client.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerBody(email))
                .retrieve()
                .toBodilessEntity()
        }

        private fun verify(client: RestClient, email: String) {
            client.post()
                .uri("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(verifyBody(email, currentCode(email)))
                .retrieve()
                .toBodilessEntity()
        }

        private fun registerAndVerify(client: RestClient, email: String) {
            register(client, email)
            verify(client, email)
        }

        private class Session(val cookie: String)

        private fun login(client: RestClient, email: String, password: String): Session {
            val response = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""{"email":"$email","password":"$password"}""")
                .retrieve()
                .toBodilessEntity()
            return Session(response.headers.getFirst(HttpHeaders.SET_COOKIE)!!.substringBefore(';'))
        }

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }
}
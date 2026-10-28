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
@Import(MutableClockConfig::class)
class AuthItSmokeTest(
    @Autowired private val jdbc: JdbcTemplate,
    @Autowired private val clock: MutableClock,
    @LocalServerPort private val port: Int,
) : StringSpec({

    beforeTest {
        clock.reset()
        jdbc.update("DELETE FROM users")
    }

    "when a new user registers then the account is stored and a session cookie is issued" {
        val client = clientFor(port)
        val response = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerBody("anna@example.com"))
            .retrieve()
            .toBodilessEntity()

        response.statusCode.value() shouldBeExactly 201
        response.headers.getFirst(HttpHeaders.SET_COOKIE) shouldContain "access_token="
        jdbc.queryForObject<Int>("SELECT count(*) FROM users WHERE email = ?", "anna@example.com")!!
            .shouldBeExactly(1)
    }

    "when the same email registers twice then the second attempt conflicts" {
        val client = clientFor(port)
        register(client, "dup@example.com")

        val status = client.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(registerBody("DUP@example.com"))
            .exchange { _, res -> res.statusCode.value() }

        status shouldBeExactly 409
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
        register(client, "login@example.com")
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
        register(client, "wrong@example.com")

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
        register(client, "out@example.com")
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
        register(client, "case@example.com")

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
        register(client, "tamper@example.com")
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
        register(client, "again@example.com")
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
        register(client, "expiry@example.com")
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

        private fun register(client: RestClient, email: String) {
            client.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerBody(email))
                .retrieve()
                .toBodilessEntity()
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
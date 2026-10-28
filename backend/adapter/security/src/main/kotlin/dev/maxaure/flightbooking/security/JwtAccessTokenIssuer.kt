package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.AccessToken
import dev.maxaure.flightbooking.application.port.out.AccessTokenIssuer
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.time.Duration
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT

internal class JwtAccessTokenIssuer(
    secret: String,
    private val ttl: Duration,
    private val clock: Clock,
) : AccessTokenIssuer {

    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    private val signer = MACSigner(key)
    private val verifier = MACVerifier(key)

    init {
        require(secret.length >= MIN_SECRET_BYTES) { "JWT secret must be at least $MIN_SECRET_BYTES characters" }
        require(!ttl.isNegative && !ttl.isZero) { "Token TTL must be positive" }
    }

    override fun issue(userId: UserId): String {
        val now = clock.instant()
        val claims = JWTClaimsSet.Builder()
            .subject(userId.value.toString())
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ttl)))
            .build()
        val token = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
        token.sign(signer)
        return token.serialize()
    }

    override fun parse(token: String): AccessToken? =
        try {
            val jwt = SignedJWT.parse(token)
            if (jwt.header.algorithm != JWSAlgorithm.HS256 || !jwt.verify(verifier)) {
                null
            } else {
                val claims = jwt.jwtClaimsSet
                val expiresAt = claims.expirationTime?.toInstant()
                when {
                    expiresAt == null || !expiresAt.isAfter(clock.instant()) -> null
                    claims.jwtid == null || claims.subject == null -> null
                    else -> AccessToken(
                        userId = UserId.fromTrusted(UUID.fromString(claims.subject)),
                        id = UUID.fromString(claims.jwtid),
                        expiresAt = expiresAt,
                    )
                }
            }
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val MIN_SECRET_BYTES = 32
    }
}

package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "airport")
internal class  AirportEntity(
    @Id
    var id: UUID,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "iata_code", length = 3)
    var iataCode: String,

    @Column(name = "name")
    var name: String,

    @Column(name = "city")
    var city: String,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", length = 2)
    var countryCode: String,

    @Column(name = "time_zone")
    var timeZone: String? = null,

    @Column(name = "source", nullable = false)
    var source: String = "manual",

    @Column(name = "synced_at")
    var syncedAt: Instant? = null,
)

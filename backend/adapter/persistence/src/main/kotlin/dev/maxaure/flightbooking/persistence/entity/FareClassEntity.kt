package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "fare_class")
internal class FareClassEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    var flight: FlightEntity,

    @Column(name = "cabin")
    var cabin: String,

    @Column(name = "base_price_amount")
    var basePriceAmount: BigDecimal,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3)
    var currency: String,

    @Column(name = "multiplier")
    var multiplier: BigDecimal,
)

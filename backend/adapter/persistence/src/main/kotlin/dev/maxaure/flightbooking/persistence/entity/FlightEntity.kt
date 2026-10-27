package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "flight")
internal class FlightEntity(
    @Id
    var id: UUID,

    @Column(name = "flight_number")
    var flightNumber: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_airport_id")
    var origin: AirportEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_airport_id")
    var destination: AirportEntity,

    @Column(name = "departure_at")
    var departureAt: Instant,

    @Column(name = "arrival_at")
    var arrivalAt: Instant,

    @Column(name = "aircraft")
    var aircraft: String,

    @Column(name = "status")
    var status: String,

    @OneToMany(mappedBy = "flight", cascade = [CascadeType.ALL], orphanRemoval = true)
    var fares: MutableList<FareClassEntity> = mutableListOf(),

    @Column(name = "source", nullable = false)
    var source: String = "manual",

    @Column(name = "synced_at")
    var syncedAt: Instant? = null,
)

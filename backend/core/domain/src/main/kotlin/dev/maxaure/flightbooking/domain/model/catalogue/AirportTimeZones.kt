package dev.maxaure.flightbooking.domain.model.catalogue

import java.time.ZoneId

object AirportTimeZones {

    val DEFAULT: ZoneId = ZoneId.of("Europe/Warsaw")

    private val byIata: Map<String, ZoneId> = mapOf(
        "WAW" to "Europe/Warsaw",
        "KRK" to "Europe/Warsaw",
        "GDN" to "Europe/Warsaw",
        "WRO" to "Europe/Warsaw",
        "POZ" to "Europe/Warsaw",
        "LIS" to "Europe/Lisbon",
        "OPO" to "Europe/Lisbon",
        "LHR" to "Europe/London",
        "LGW" to "Europe/London",
        "DUB" to "Europe/Dublin",
        "CDG" to "Europe/Paris",
        "ORY" to "Europe/Paris",
        "BER" to "Europe/Berlin",
        "MUC" to "Europe/Berlin",
        "FRA" to "Europe/Berlin",
        "AMS" to "Europe/Amsterdam",
        "BRU" to "Europe/Brussels",
        "MAD" to "Europe/Madrid",
        "BCN" to "Europe/Madrid",
        "FCO" to "Europe/Rome",
        "MXP" to "Europe/Rome",
        "ATH" to "Europe/Athens",
        "IST" to "Europe/Istanbul",
        "JFK" to "America/New_York",
        "EWR" to "America/New_York",
        "LAX" to "America/Los_Angeles",
        "SFO" to "America/Los_Angeles",
        "ORD" to "America/Chicago",
        "DXB" to "Asia/Dubai",
        "SIN" to "Asia/Singapore",
        "HND" to "Asia/Tokyo",
        "NRT" to "Asia/Tokyo",
        "SYD" to "Australia/Sydney",
    ).mapValues { (_, zone) -> ZoneId.of(zone) }

    fun of(iataCode: String): ZoneId = byIata[iataCode] ?: DEFAULT
}

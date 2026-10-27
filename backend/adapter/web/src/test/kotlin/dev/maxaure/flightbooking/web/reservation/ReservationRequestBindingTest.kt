package dev.maxaure.flightbooking.web.reservation

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals

class ReservationRequestBindingTest {

    private val jsonMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @Test
    fun `when a hold request arrives as json then every seat and passenger name is bound`() {
        val body = """
            {"seats":[{"seat":"12A","passengerName":"Ada Lovelace"},{"seat":"12B","passengerName":"Alan Turing"}]}
        """.trimIndent()

        val request = jsonMapper.readValue(body, HoldSeatsRequest::class.java)

        assertEquals(2, request.seats.size)
        assertEquals("12A", request.seats[0].seat)
        assertEquals("Alan Turing", request.seats[1].passengerName)
    }
}

package dev.maxaure.flightbooking.web.reservation

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals

class ReservationRequestBindingTest {

    private val jsonMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @Test
    fun `when a hold request arrives as json then every seat and its passenger type are bound`() {
        val body = """
            {"seats":[{"seat":"12A","passengerType":"SELF"},{"seat":"12B","passengerType":"OTHER","firstName":"Alan","lastName":"Turing"}]}
        """.trimIndent()

        val request = jsonMapper.readValue(body, HoldSeatsRequest::class.java)

        assertEquals(2, request.seats.size)
        assertEquals("12B", request.seats[1].seat)
        assertEquals("OTHER", request.seats[1].passengerType?.name)
        assertEquals("Alan", request.seats[1].firstName)
        assertEquals("Turing", request.seats[1].lastName)
    }
}

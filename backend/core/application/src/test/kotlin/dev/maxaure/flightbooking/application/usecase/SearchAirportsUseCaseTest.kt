package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.AirportDirectory
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchAirportsUseCaseTest {

    private fun airport(code: String, name: String, city: String) = Airport.create(
        iataCode = code,
        name = name,
        city = city,
        countryCode = "PL",
    )

    private fun useCase(vararg airports: Airport) = SearchAirportsUseCase(FakeDirectory(airports.toList()))

    @Test
    fun `when the query is blank then no airports are returned`() {
        val results = useCase(airport("WAW", "Warsaw Chopin Airport", "Warsaw")).search("   ")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `when a city prefix is typed then matching airports come back with codes and country`() {
        val results = useCase(
            airport("WAW", "Warsaw Chopin Airport", "Warsaw"),
            airport("WMI", "Warsaw Modlin Airport", "Warsaw"),
            airport("GDN", "Gdansk Lech Walesa Airport", "Gdansk"),
        ).search("warsaw")

        assertEquals(listOf("WAW", "WMI"), results.map { it.iataCode })
        assertEquals("Warsaw Chopin Airport", results.first().name)
        assertEquals("PL", results.first().countryCode)
    }

    @Test
    fun `when an airport code is typed then the query reaches the directory trimmed`() {
        val directory = FakeDirectory(listOf(airport("WAW", "Warsaw Chopin Airport", "Warsaw")))

        SearchAirportsUseCase(directory).search("  WA  ")

        assertEquals(listOf("WA"), directory.queries.map { it.first })
    }

    @Test
    fun `when more matches exist than the limit then only the limit is returned`() {
        val airports = ('A'..'T').map { airport("W$it" + "W", "Warsaw Airport $it", "Warsaw") }.toTypedArray()

        val results = useCase(*airports).search("warsaw")

        assertEquals(10, results.size)
    }

    @Test
    fun `when results come back unordered then they are sorted by code`() {
        val results = useCase(
            airport("WMI", "Warsaw Modlin Airport", "Warsaw"),
            airport("WAW", "Warsaw Chopin Airport", "Warsaw"),
        ).search("warsaw")

        assertEquals(listOf("WAW", "WMI"), results.map { it.iataCode })
    }
}

private class FakeDirectory(
    private val airports: List<Airport>,
) : AirportDirectory {

    val queries = mutableListOf<Pair<String, Int>>()

    override fun search(query: String, limit: Int): List<Airport> {
        queries += query to limit
        val needle = query.lowercase()
        return airports
            .filter {
                it.iataCode.value.lowercase().contains(needle) ||
                    it.city.lowercase().contains(needle) ||
                    it.name.lowercase().contains(needle)
            }
            .take(limit)
    }
}

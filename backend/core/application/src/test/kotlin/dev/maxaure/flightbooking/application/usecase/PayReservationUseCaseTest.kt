package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.PaymentRequest
import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.application.port.out.PaymentCommand
import dev.maxaure.flightbooking.application.port.out.PaymentGateway
import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.application.port.out.PaymentResult
import dev.maxaure.flightbooking.application.port.out.ReservationPricer
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.catalogue.FlightIdentity
import dev.maxaure.flightbooking.domain.model.catalogue.FlightStatus
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.domain.model.payment.PaymentStatus
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.domain.model.seat.Seat
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PayReservationUseCaseTest {

    private var now = Instant.parse("2026-10-27T10:00:00Z")
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }

    private val userId = UserId.new()
    private val flightId = UUID.randomUUID()
    private val price = Price(BigDecimal("250.00"), "PLN")
    private val user = User.verified(Email.of("${userId.value}@example.com"), PasswordHash.fromTrusted("hash"), now, userId)
    private val flight = Flight(
        id = flightId,
        flightNumber = FlightNumber.of("LO381"),
        origin = Airport.create("WAW", "Warsaw Chopin", "Warsaw", "PL"),
        destination = Airport.create("LIS", "Lisbon", "Lisbon", "PT"),
        departureAt = now.plus(Duration.ofDays(3)),
        arrivalAt = now.plus(Duration.ofDays(3)).plus(Duration.ofHours(4)),
        aircraft = "A320neo",
        status = FlightStatus.SCHEDULED,
        fares = listOf(FareClass(UUID.randomUUID(), Cabin.ECONOMY, price)),
    )
    private val users = object : UserRepository {
        override fun findById(id: UserId): User? = user.takeIf { it.id == id }
        override fun findByEmail(email: Email): User? = user.takeIf { it.email == email }
        override fun save(user: User): User = user
    }
    private val flights = object : FlightRepository {
        override fun findById(id: UUID): Flight? = flight.takeIf { it.id == id }
        override fun findByIdentity(identity: FlightIdentity): Flight? = flight.takeIf { it.identity == identity }
        override fun save(flight: Flight): Flight = flight
        override fun replaceSeatInventory(flightId: UUID, seats: List<Seat>) = Unit
        override fun countSeats(flightId: UUID): Int = 0
        override fun countSoldSeats(flightId: UUID): Int = 0
        override fun findDeparturesBetween(from: Instant, before: Instant): List<Flight> = emptyList()
    }
    private val published = mutableListOf<NotificationEvent>()
    private val notifications = object : NotificationPublisher {
        override fun publish(event: NotificationEvent) {
            published += event
        }
    }
    private val reservations = InMemoryReservationRepository { now }
    private val payments = InMemoryPaymentRepository()
    private val pricer = object : ReservationPricer {
        override fun priceOf(reservationId: UUID): Price? = price
    }
    private val gateway = StubPaymentGateway(PaymentResult.Approved("auth-1"))
    private val useCase = PayReservationUseCase(
        reservations, payments, pricer, gateway, NoConversion, users, flights, notifications, clock,
    )

    private val eurConverter = object : CurrencyConverter {
        override val defaultCurrency: String = "PLN"
        override val supportedCurrencies: List<String> = listOf("PLN", "EUR")

        override fun convert(price: Price, target: String?): Price =
            if (target == null) price
            else Price(price.amount.multiply(BigDecimal("0.23")).setScale(2, RoundingMode.HALF_UP), target)
    }
    private val eurUseCase = PayReservationUseCase(
        reservations, payments, pricer, gateway, eurConverter, users, flights, notifications, clock,
    )

    private fun hold(userId: UserId = this.userId, seat: String = "12A"): Reservation =
        reservations.holdSeats(
            Reservation.hold(
                flightId = flightId,
                seats = listOf(PassengerSeat(SeatNumber.of(seat), "Ada", "Lovelace")),
                now = now,
                userId = userId.value,
            ),
        ).let { (it as HoldOutcome.Held).reservation }

    private fun request(key: String = "key-1", card: String = "4242424242424242") =
        PaymentRequest(idempotencyKey = key, cardNumber = card, cardHolder = "Ada Lovelace")

    @Test
    fun `when the gateway approves then the reservation is confirmed and the hold expiry is gone`() {
        val reservation = hold()

        val payment = useCase.pay(userId, reservation.id, request())

        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals("4242", payment.cardLast4)
        assertEquals(price, payment.amount)
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(reservation.id)?.status)
        assertNull(reservations.findById(reservation.id)?.expiresAt)
    }

    @Test
    fun `when the gateway approves then the owner receives a confirmation for the held seats`() {
        val reservation = hold(seat = "12A")

        useCase.pay(userId, reservation.id, request())

        val event = published.single() as NotificationEvent.ReservationConfirmed
        assertEquals(user.email, event.recipient)
        assertEquals(reservation.id, event.reservationId)
        assertEquals(flight.flightNumber, event.flightNumber)
        assertEquals(listOf(SeatNumber.of("12A")), event.seats)
    }

    @Test
    fun `when the gateway declines then the owner receives no confirmation`() {
        gateway.result = PaymentResult.Declined("Card declined by issuer")
        val reservation = hold()

        useCase.pay(userId, reservation.id, request())

        assertEquals(emptyList(), published)
    }

    @Test
    fun `when the same idempotency key is replayed then the owner is not notified twice`() {
        val reservation = hold()
        useCase.pay(userId, reservation.id, request())

        useCase.pay(userId, reservation.id, request())

        assertEquals(1, published.size)
    }

    @Test
    fun `when the gateway declines then the reservation keeps its hold and stays pending`() {
        gateway.result = PaymentResult.Declined("Card declined by issuer")
        val reservation = hold()

        val payment = useCase.pay(userId, reservation.id, request())

        assertEquals(PaymentStatus.DECLINED, payment.status)
        assertEquals("Card declined by issuer", payment.declineReason)
        assertEquals(ReservationStatus.PENDING, reservations.findById(reservation.id)?.status)
        assertEquals(reservation.expiresAt, reservations.findById(reservation.id)?.expiresAt)
    }

    @Test
    fun `when the same idempotency key is retried then the gateway is charged once and the same payment returns`() {
        val reservation = hold()
        val first = useCase.pay(userId, reservation.id, request())

        val replay = useCase.pay(userId, reservation.id, request())

        assertEquals(first.id, replay.id)
        assertEquals(1, gateway.calls.size)
    }

    @Test
    fun `when a target currency is chosen then the gateway is charged the converted amount`() {
        val reservation = hold()

        val payment = eurUseCase.pay(userId, reservation.id, request().copy(currency = "EUR"))

        assertEquals(Price(BigDecimal("57.50"), "EUR"), gateway.calls.single().amount)
        assertEquals(Price(BigDecimal("57.50"), "EUR"), payment.amount)
    }

    @Test
    fun `when the hold has expired then the payment is rejected before the gateway is called`() {
        val reservation = hold()
        now = now.plus(Duration.ofMinutes(15))

        assertFailsWith<ConflictException> { useCase.pay(userId, reservation.id, request()) }

        assertEquals(0, gateway.calls.size)
        assertEquals(ReservationStatus.PENDING, reservations.findById(reservation.id)?.status)
    }

    @Test
    fun `when a stranger pays someone else's reservation then it is reported as not found and nothing is charged`() {
        val reservation = hold()

        assertFailsWith<NotFoundException> { useCase.pay(UserId.new(), reservation.id, request()) }

        assertEquals(0, gateway.calls.size)
    }

    @Test
    fun `when a confirmed reservation is paid again with a new key then it is rejected`() {
        val reservation = hold()
        useCase.pay(userId, reservation.id, request("key-1"))

        assertFailsWith<ConflictException> { useCase.pay(userId, reservation.id, request("key-2")) }

        assertEquals(1, gateway.calls.size)
    }

    @Test
    fun `when a key already used for another reservation is replayed then it is rejected`() {
        val first = hold(seat = "12A")
        val second = hold(seat = "12B")
        gateway.result = PaymentResult.Declined("Card declined by issuer")
        useCase.pay(userId, first.id, request("shared-key"))

        assertFailsWith<ConflictException> { useCase.pay(userId, second.id, request("shared-key")) }
    }

    private class StubPaymentGateway(var result: PaymentResult) : PaymentGateway {
        val calls = mutableListOf<PaymentCommand>()

        override fun authorize(command: PaymentCommand): PaymentResult {
            calls += command
            return result
        }
    }

    private class InMemoryPaymentRepository : PaymentRepository {
        private val byKey = linkedMapOf<String, Payment>()

        override fun findByIdempotencyKey(key: String): Payment? = byKey[key]

        override fun findLatestApproved(reservationId: UUID): Payment? =
            byKey.values
                .filter { it.reservationId == reservationId && it.status == PaymentStatus.APPROVED }
                .maxByOrNull { it.createdAt }

        override fun save(payment: Payment): Payment = payment.also { byKey[it.idempotencyKey] = it }
    }

    private class InMemoryReservationRepository(
        private val now: () -> Instant,
    ) : ReservationRepository {
        private val reservations = linkedMapOf<UUID, Reservation>()

        override fun findById(id: UUID): Reservation? = reservations[id]

        override fun save(reservation: Reservation): Reservation = reservation.also { reservations[it.id] = it }

        override fun holdSeats(reservation: Reservation): HoldOutcome {
            reservations[reservation.id] = reservation
            return HoldOutcome.Held(reservation)
        }

        override fun releaseSeats(reservationId: UUID): Reservation? = error("Not used")

        override fun releaseSeatSubset(reservationId: UUID, seatNumbers: List<String>): Reservation? =
            error("Not used")

        override fun updatePassengers(reservationId: UUID, seats: List<PassengerSeat>): Reservation? {
            val current = reservations[reservationId] ?: return null
            return current.copy(seats = seats).also { reservations[reservationId] = it }
        }

        override fun confirm(reservationId: UUID): Reservation? {
            val current = reservations[reservationId] ?: return null
            return current.pay(now()).also { reservations[reservationId] = it }
        }

        override fun findExpired(at: Instant, limit: Int): List<Reservation> = emptyList()

        override fun findByUserId(userId: UUID): List<Reservation> =
            reservations.values.filter { it.userId == userId }
    }
}

package dev.maxaure.flightbooking.domain.model.user

import dev.maxaure.flightbooking.domain.DomainException
import java.time.Duration

class TooManyLoginAttempts(val retryAfter: Duration) :
    DomainException("Too many failed login attempts; retry in ${retryAfter.seconds} seconds")

package dev.maxaure.flightbooking.domain

open class DomainException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidDomainValueException(message: String, cause: Throwable? = null) : DomainException(message, cause)

class NotFoundException(message: String) : DomainException(message)

class ConflictException(message: String) : DomainException(message)

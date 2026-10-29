package dev.maxaure.flightbooking.domain.model.user

import dev.maxaure.flightbooking.domain.DomainException

class EmailAlreadyRegistered(val email: Email) :
    DomainException("An account with email ${email.value} already exists")

class InvalidCredentials : DomainException("Invalid credentials")

class EmailNotVerified : DomainException("Email address is not verified")

class ProfileIncomplete : DomainException("Add your first and last name before booking for yourself")

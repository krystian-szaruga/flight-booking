package dev.maxaure.flightbooking.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["dev.maxaure.flightbooking"])
@EntityScan("dev.maxaure.flightbooking.persistence.entity")
class FlightBookingApplication

fun main(args: Array<String>) {
    runApplication<FlightBookingApplication>(*args)
}

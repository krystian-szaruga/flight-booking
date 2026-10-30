package dev.maxaure.flightbooking.application.port.`in`

interface SendDueNotifications {

    fun sendPaymentReminders(): Int

    fun sendDepartureReminders(): Int
}

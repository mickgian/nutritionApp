package com.meridia.shared.viewModels

import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.CancellationDto
import com.meridia.shared.models.SlotDto
import com.meridia.shared.network.AppointmentRepository

/** In-memory [AppointmentRepository] for BookingViewModel / ProfileViewModel tests. */
class FakeAppointmentRepository(
    private val slots: List<SlotDto> = emptyList(),
    private val availabilityError: Throwable? = null,
    private val bookResult: AppointmentDto? = null,
    private val bookError: Throwable? = null,
    private val appointments: List<AppointmentDto> = emptyList(),
    private val cancelResult: CancellationDto? = null,
    private val cancelError: Throwable? = null,
) : AppointmentRepository {

    var lastCancelledId: Int? = null
        private set

    override suspend fun availability(fromDate: String, toDate: String): List<SlotDto> {
        availabilityError?.let { throw it }
        return slots
    }

    override suspend fun book(slotId: Int, visitType: String): AppointmentDto {
        bookError?.let { throw it }
        return bookResult
            ?: AppointmentDto(1, slotId, visitType, "2026-07-28T10:00:00", "pending_payment", 9000)
    }

    override suspend fun myAppointments(): List<AppointmentDto> = appointments

    override suspend fun cancel(appointmentId: Int): CancellationDto {
        lastCancelledId = appointmentId
        cancelError?.let { throw it }
        return cancelResult ?: CancellationDto(appointmentId, "cancelled", null)
    }
}

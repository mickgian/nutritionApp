package com.meridia.shared.viewModels

import com.meridia.shared.models.AdminClientDto
import com.meridia.shared.models.AdminOrderDto
import com.meridia.shared.models.AdminSlotDto
import com.meridia.shared.models.PromotionResultDto
import com.meridia.shared.network.AdminRepository

/** In-memory [AdminRepository] for studio-panel ViewModel tests. */
class FakeAdminRepository(
    private var clients: List<AdminClientDto> = emptyList(),
    private val orders: List<AdminOrderDto> = emptyList(),
    private var slots: List<AdminSlotDto> = emptyList(),
    private val loadError: Throwable? = null,
    private val assignError: Throwable? = null,
    private val promotionError: Throwable? = null,
    private val toggleError: Throwable? = null,
    private val promotionRecipients: Int = 0,
) : AdminRepository {

    var lastAssignedClientId: Int? = null
        private set
    var lastAssignedPlan: String? = null
        private set
    var lastPromotionTitle: String? = null
        private set
    var lastToggledStartsAt: String? = null
        private set
    var lastToggledIsOpen: Boolean? = null
        private set

    override suspend fun clients(): List<AdminClientDto> {
        loadError?.let { throw it }
        return clients
    }

    override suspend fun orders(): List<AdminOrderDto> {
        loadError?.let { throw it }
        return orders
    }

    override suspend fun assignPlan(clientId: Int, name: String, dailyKcal: Int, weeks: Int) {
        assignError?.let { throw it }
        lastAssignedClientId = clientId
        lastAssignedPlan = name
        // Reflect the assignment so a reload shows the new active plan.
        clients = clients.map { if (it.id == clientId) it.copy(activePlan = name) else it }
    }

    override suspend fun sendPromotion(
        title: String,
        body: String,
        icon: String,
    ): PromotionResultDto {
        lastPromotionTitle = title
        promotionError?.let { throw it }
        return PromotionResultDto(recipients = promotionRecipients)
    }

    override suspend fun availability(fromDate: String, toDate: String): List<AdminSlotDto> {
        loadError?.let { throw it }
        return slots
    }

    override suspend fun setAvailability(
        startsAt: String,
        durationMin: Int,
        isOpen: Boolean,
    ): AdminSlotDto {
        toggleError?.let { throw it }
        lastToggledStartsAt = startsAt
        lastToggledIsOpen = isOpen
        val id = slots.firstOrNull { it.startsAt == startsAt }?.id ?: 1
        return AdminSlotDto(id = id, startsAt = startsAt, durationMin = durationMin, isOpen = isOpen)
    }
}

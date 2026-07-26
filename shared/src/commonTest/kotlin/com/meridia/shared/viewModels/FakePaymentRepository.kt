package com.meridia.shared.viewModels

import com.meridia.shared.models.PaymentDto
import com.meridia.shared.network.PaymentRepository

/** In-memory [PaymentRepository] for booking / box-checkout ViewModel tests. */
class FakePaymentRepository(
    private val result: PaymentDto? = null,
    private val error: Throwable? = null,
) : PaymentRepository {

    var lastTarget: String? = null
        private set
    var lastTargetId: Int? = null
        private set
    var lastMethod: String? = null
        private set

    override suspend fun pay(target: String, targetId: Int, method: String, token: String): PaymentDto {
        lastTarget = target
        lastTargetId = targetId
        lastMethod = method
        error?.let { throw it }
        return result ?: PaymentDto(
            id = 1,
            amountCents = 9000,
            method = method,
            status = "succeeded",
            createdAt = "2026-07-26T10:00:00Z",
        )
    }
}

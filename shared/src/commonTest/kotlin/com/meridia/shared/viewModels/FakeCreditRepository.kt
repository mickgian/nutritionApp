package com.meridia.shared.viewModels

import com.meridia.shared.models.CreditDto
import com.meridia.shared.network.CreditRepository

/** In-memory [CreditRepository] for ProfileViewModel tests. */
class FakeCreditRepository(
    private val credits: List<CreditDto> = emptyList(),
    private val error: Throwable? = null,
) : CreditRepository {

    override suspend fun myCredits(): List<CreditDto> {
        error?.let { throw it }
        return credits
    }
}

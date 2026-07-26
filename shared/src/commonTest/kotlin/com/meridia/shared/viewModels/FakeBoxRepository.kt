package com.meridia.shared.viewModels

import com.meridia.shared.models.BoxDto
import com.meridia.shared.models.PlanDto
import com.meridia.shared.network.BoxRepository

/** In-memory [BoxRepository] for BoxViewModel tests. */
class FakeBoxRepository(
    private val plan: PlanDto? = null,
    private val box: BoxDto = BoxDto(weekIndex = 0, items = emptyList()),
    private val planError: Throwable? = null,
    private val boxError: Throwable? = null,
) : BoxRepository {

    override suspend fun myPlan(): PlanDto? {
        planError?.let { throw it }
        return plan
    }

    override suspend fun box(week: Int): BoxDto {
        boxError?.let { throw it }
        return box
    }
}

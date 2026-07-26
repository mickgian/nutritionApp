package com.meridia.shared.viewModels

import com.meridia.shared.models.MealDto
import com.meridia.shared.network.MealRepository

/** In-memory [MealRepository] for MealDetailViewModel tests. */
class FakeMealRepository(
    private val result: MealDto? = null,
    private val error: Throwable? = null,
) : MealRepository {

    override suspend fun meal(id: Int): MealDto {
        error?.let { throw it }
        return result ?: error("No result configured")
    }
}

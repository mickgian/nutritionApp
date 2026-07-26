package com.meridia.shared.viewModels

import com.meridia.shared.models.ProfessionalDto
import com.meridia.shared.network.ProfessionalRepository

/** In-memory [ProfessionalRepository] for ConsulenzaViewModel tests. */
class FakeProfessionalRepository(
    private val result: ProfessionalDto? = null,
    private val error: Throwable? = null,
) : ProfessionalRepository {

    override suspend fun profile(): ProfessionalDto {
        error?.let { throw it }
        return result ?: error("No result configured")
    }
}

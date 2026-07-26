package com.meridia.shared.viewModels

import com.meridia.shared.network.PrivacyRepository

/** In-memory [PrivacyRepository] for ProfileViewModel tests. */
class FakePrivacyRepository(
    private val exportResult: String = "{}",
    private val exportError: Throwable? = null,
    private val deleteError: Throwable? = null,
) : PrivacyRepository {

    var exportCalls = 0
    var deleteCalls = 0

    override suspend fun exportData(): String {
        exportCalls++
        exportError?.let { throw it }
        return exportResult
    }

    override suspend fun deleteAccount() {
        deleteCalls++
        deleteError?.let { throw it }
    }
}

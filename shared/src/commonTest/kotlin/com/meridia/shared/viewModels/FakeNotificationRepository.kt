package com.meridia.shared.viewModels

import com.meridia.shared.models.NotificationDto
import com.meridia.shared.network.NotificationRepository

/** In-memory [NotificationRepository] for NotificationsViewModel tests. */
class FakeNotificationRepository(
    private val notifications: List<NotificationDto> = emptyList(),
    private val error: Throwable? = null,
) : NotificationRepository {

    var markAllReadCalls: Int = 0
        private set

    override suspend fun myNotifications(): List<NotificationDto> {
        error?.let { throw it }
        return notifications
    }

    override suspend fun markAllRead(): Int {
        markAllReadCalls += 1
        return notifications.count { !it.isRead }
    }
}

package com.meridia.shared.viewModels

import com.meridia.shared.models.NotificationDto
import com.meridia.shared.network.BookingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationsViewModelTest {

    private fun notification(id: Int, read: Boolean) = NotificationDto(
        id = id,
        kind = "order_deadline",
        title = "Ordina il box",
        body = "Conferma entro giovedì.",
        icon = "🍱",
        isRead = read,
        createdAt = "2026-07-26T10:00:00Z",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_emits_content_with_unread_count() = runTest {
        val vm = NotificationsViewModel(
            FakeNotificationRepository(listOf(notification(1, read = false), notification(2, read = true))),
        )
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is NotificationsUiState.Content)
        assertEquals(2, (state as NotificationsUiState.Content).notifications.size)
        assertEquals(1, state.unreadCount)
    }

    @Test
    fun load_empty_emits_empty_content() = runTest {
        val vm = NotificationsViewModel(FakeNotificationRepository(emptyList()))
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is NotificationsUiState.Content)
        assertTrue((state as NotificationsUiState.Content).notifications.isEmpty())
    }

    @Test
    fun load_failure_emits_italian_error() = runTest {
        val vm = NotificationsViewModel(
            FakeNotificationRepository(error = BookingException("Impossibile caricare le notifiche. Riprova.")),
        )
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is NotificationsUiState.Error)
        assertEquals("Impossibile caricare le notifiche. Riprova.", (state as NotificationsUiState.Error).message)
    }

    @Test
    fun load_with_mark_read_marks_when_unread_present() = runTest {
        val repo = FakeNotificationRepository(listOf(notification(1, read = false)))
        val vm = NotificationsViewModel(repo)
        vm.load(markRead = true)
        advanceUntilIdle()
        assertEquals(1, repo.markAllReadCalls)
    }

    @Test
    fun load_with_mark_read_skips_when_all_read() = runTest {
        val repo = FakeNotificationRepository(listOf(notification(1, read = true)))
        val vm = NotificationsViewModel(repo)
        vm.load(markRead = true)
        advanceUntilIdle()
        assertEquals(0, repo.markAllReadCalls)
    }

    @Test
    fun load_without_mark_read_never_marks() = runTest {
        val repo = FakeNotificationRepository(listOf(notification(1, read = false)))
        val vm = NotificationsViewModel(repo)
        vm.load()
        advanceUntilIdle()
        assertEquals(0, repo.markAllReadCalls)
    }
}

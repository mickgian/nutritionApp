package com.meridia.shared.viewModels

import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.CancellationDto
import com.meridia.shared.models.CreditDto
import com.meridia.shared.models.OrderDto
import com.meridia.shared.models.PlanDto
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTest {

    private val plan = PlanDto(id = 1, name = "Dimagrimento 1400 kcal", dailyKcal = 1400, weeks = 4)
    private val appointment =
        AppointmentDto(9, 3, "prima", "2026-08-10T10:00:00+00:00", "pending_payment", 9000)
    private val credit =
        CreditDto(2, 9000, "Annullamento appuntamento", 9, "2026-07-26T10:00:00Z", "2027-01-26T00:00:00Z")

    private fun vm(
        appointments: FakeAppointmentRepository = FakeAppointmentRepository(),
        credits: FakeCreditRepository = FakeCreditRepository(),
        boxes: FakeBoxRepository = FakeBoxRepository(),
        orders: FakeOrderRepository = FakeOrderRepository(),
        privacy: FakePrivacyRepository = FakePrivacyRepository(),
    ) = ProfileViewModel(appointments, credits, boxes, orders, privacy)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_aggregates_me_data() = runTest {
        val model = vm(
            appointments = FakeAppointmentRepository(appointments = listOf(appointment)),
            credits = FakeCreditRepository(credits = listOf(credit)),
            boxes = FakeBoxRepository(plan = plan),
            orders = FakeOrderRepository(
                orders = listOf(
                    OrderDto(1, "subscription", "mattina", "pending_payment", 7900, "2026-07-26T10:00:00Z"),
                ),
            ),
        )
        model.load()
        advanceUntilIdle()
        val state = model.state.value
        assertTrue(state is ProfileUiState.Content)
        state as ProfileUiState.Content
        assertEquals(9, state.upcomingAppointment?.id)
        assertEquals(1, state.credits.size)
        assertEquals("Dimagrimento 1400 kcal", state.plan?.name)
        assertEquals(1, state.boxCount)
        assertTrue(state.subscriptionActive)
    }

    @Test
    fun load_empty_renders_content_with_defaults() = runTest {
        val model = vm()
        model.load()
        advanceUntilIdle()
        val state = model.state.value
        assertTrue(state is ProfileUiState.Content)
        state as ProfileUiState.Content
        assertNull(state.upcomingAppointment)
        assertTrue(state.credits.isEmpty())
        assertNull(state.plan)
        assertEquals(0, state.boxCount)
        assertTrue(!state.subscriptionActive)
    }

    @Test
    fun cancelled_appointment_is_not_shown_as_upcoming() = runTest {
        val cancelled = appointment.copy(status = "cancelled")
        val model = vm(appointments = FakeAppointmentRepository(appointments = listOf(cancelled)))
        model.load()
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertNull(state.upcomingAppointment)
    }

    @Test
    fun cancel_with_credit_sets_notice_and_calls_repo() = runTest {
        val repo = FakeAppointmentRepository(
            appointments = listOf(appointment),
            cancelResult = CancellationDto(9, "cancelled", credit),
        )
        val model = vm(appointments = repo)
        model.load()
        advanceUntilIdle()
        model.cancel(9)
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertEquals(9, repo.lastCancelledId)
        assertTrue(state.notice?.contains("credito") == true)
        assertTrue(!state.cancelling)
    }

    @Test
    fun cancel_without_credit_sets_no_refund_notice() = runTest {
        val repo = FakeAppointmentRepository(
            appointments = listOf(appointment),
            cancelResult = CancellationDto(9, "cancelled", null),
        )
        val model = vm(appointments = repo)
        model.load()
        advanceUntilIdle()
        model.cancel(9)
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertTrue(state.notice?.contains("Nessun rimborso") == true)
    }

    @Test
    fun cancel_failure_sets_error_notice_without_losing_content() = runTest {
        val repo = FakeAppointmentRepository(
            appointments = listOf(appointment),
            cancelError = BookingException("Appuntamento non trovato"),
        )
        val model = vm(appointments = repo)
        model.load()
        advanceUntilIdle()
        model.cancel(9)
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertEquals("Appuntamento non trovato", state.notice)
        assertTrue(!state.cancelling)
    }

    @Test
    fun load_failure_emits_error() = runTest {
        val model = vm(credits = FakeCreditRepository(error = BookingException("Errore di rete")))
        model.load()
        advanceUntilIdle()
        assertTrue(model.state.value is ProfileUiState.Error)
    }

    @Test
    fun export_success_stores_data_and_sets_notice() = runTest {
        val privacy = FakePrivacyRepository(exportResult = "{\"account\":{}}")
        val model = vm(privacy = privacy)
        model.load()
        advanceUntilIdle()
        model.exportData()
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertEquals(1, privacy.exportCalls)
        assertEquals("{\"account\":{}}", state.exportedJson)
        assertTrue(state.privacyNotice?.contains("Esportazione") == true)
        assertTrue(!state.privacyBusy)
    }

    @Test
    fun export_failure_sets_privacy_notice() = runTest {
        val model = vm(privacy = FakePrivacyRepository(exportError = BookingException("Errore di rete")))
        model.load()
        advanceUntilIdle()
        model.exportData()
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertEquals("Errore di rete", state.privacyNotice)
        assertNull(state.exportedJson)
        assertTrue(!state.privacyBusy)
    }

    @Test
    fun delete_success_invokes_callback() = runTest {
        val privacy = FakePrivacyRepository()
        val model = vm(privacy = privacy)
        model.load()
        advanceUntilIdle()
        var deleted = false
        model.deleteAccount { deleted = true }
        advanceUntilIdle()
        assertEquals(1, privacy.deleteCalls)
        assertTrue(deleted)
    }

    @Test
    fun delete_failure_keeps_user_and_shows_notice() = runTest {
        val model = vm(privacy = FakePrivacyRepository(deleteError = BookingException("Sessione scaduta")))
        model.load()
        advanceUntilIdle()
        var deleted = false
        model.deleteAccount { deleted = true }
        advanceUntilIdle()
        val state = model.state.value as ProfileUiState.Content
        assertTrue(!deleted)
        assertEquals("Sessione scaduta", state.privacyNotice)
        assertTrue(!state.privacyBusy)
    }
}

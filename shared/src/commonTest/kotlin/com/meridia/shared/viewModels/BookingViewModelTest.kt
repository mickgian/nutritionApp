package com.meridia.shared.viewModels

import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.SlotDto
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

class BookingViewModelTest {

    private val slot = SlotDto(id = 1, startsAt = "2026-07-28T10:00:00", durationMin = 60)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_with_slots_emits_content() = runTest {
        val vm = BookingViewModel(FakeAppointmentRepository(slots = listOf(slot)))
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is BookingUiState.Content)
        assertEquals(1, (state as BookingUiState.Content).slots.size)
    }

    @Test
    fun load_no_slots_emits_empty() = runTest {
        val vm = BookingViewModel(FakeAppointmentRepository(slots = emptyList()))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is BookingUiState.Empty)
    }

    @Test
    fun load_failure_emits_error() = runTest {
        val vm = BookingViewModel(FakeAppointmentRepository(availabilityError = BookingException("Errore di rete")))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is BookingUiState.Error)
    }

    @Test
    fun select_then_next_advances_to_riepilogo() = runTest {
        val vm = BookingViewModel(FakeAppointmentRepository(slots = listOf(slot)))
        vm.load()
        advanceUntilIdle()
        vm.selectSlot(slot)
        vm.next()
        val state = vm.state.value
        assertTrue(state is BookingUiState.Content)
        assertEquals(BookingStep.Riepilogo, (state as BookingUiState.Content).step)
    }

    @Test
    fun confirm_success_emits_confirmed() = runTest {
        val appointment = AppointmentDto(9, slot.id, "prima", slot.startsAt, "pending_payment", 9000)
        val vm = BookingViewModel(
            FakeAppointmentRepository(slots = listOf(slot), bookResult = appointment),
        )
        vm.load()
        advanceUntilIdle()
        vm.selectSlot(slot)
        vm.confirm()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is BookingUiState.Confirmed)
        assertEquals(9, (state as BookingUiState.Confirmed).appointment.id)
    }

    @Test
    fun confirm_failure_sets_italian_submit_error() = runTest {
        val vm = BookingViewModel(
            FakeAppointmentRepository(
                slots = listOf(slot),
                bookError = BookingException("Orario non più disponibile"),
            ),
        )
        vm.load()
        advanceUntilIdle()
        vm.selectSlot(slot)
        vm.confirm()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is BookingUiState.Content)
        assertEquals("Orario non più disponibile", (state as BookingUiState.Content).submitError)
    }
}

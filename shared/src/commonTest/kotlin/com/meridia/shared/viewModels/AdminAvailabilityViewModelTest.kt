package com.meridia.shared.viewModels

import com.meridia.shared.models.AdminSlotDto
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

class AdminAvailabilityViewModelTest {

    private val slots = listOf(
        AdminSlotDto(id = 1, startsAt = "2026-08-01T09:00:00", durationMin = 60, isOpen = true),
        AdminSlotDto(id = 2, startsAt = "2026-08-01T10:00:00", durationMin = 60, isOpen = false),
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
    fun load_success_emits_content() = runTest {
        val vm = AdminAvailabilityViewModel(FakeAdminRepository(slots = slots))
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is AdminAvailabilityUiState.Content)
        assertEquals(2, (state as AdminAvailabilityUiState.Content).slots.size)
    }

    @Test
    fun load_failure_emits_error() = runTest {
        val vm = AdminAvailabilityViewModel(FakeAdminRepository(loadError = BookingException("Errore")))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is AdminAvailabilityUiState.Error)
    }

    @Test
    fun toggle_flips_the_slot_open_state() = runTest {
        val repo = FakeAdminRepository(slots = slots)
        val vm = AdminAvailabilityViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.toggle(slots[0]) // currently open → close
        advanceUntilIdle()
        val content = vm.state.value as AdminAvailabilityUiState.Content
        assertEquals("2026-08-01T09:00:00", repo.lastToggledStartsAt)
        assertEquals(false, repo.lastToggledIsOpen)
        assertEquals(false, content.slots.first { it.id == 1 }.isOpen)
        assertNull(content.togglingSlotId)
    }

    @Test
    fun toggle_failure_sets_error_and_keeps_slots() = runTest {
        val vm = AdminAvailabilityViewModel(
            FakeAdminRepository(slots = slots, toggleError = BookingException("Orario già prenotato, impossibile chiuderlo")),
        )
        vm.load()
        advanceUntilIdle()
        vm.toggle(slots[0])
        advanceUntilIdle()
        val content = vm.state.value as AdminAvailabilityUiState.Content
        assertEquals("Orario già prenotato, impossibile chiuderlo", content.error)
        assertEquals(2, content.slots.size)
        assertNull(content.togglingSlotId)
    }
}

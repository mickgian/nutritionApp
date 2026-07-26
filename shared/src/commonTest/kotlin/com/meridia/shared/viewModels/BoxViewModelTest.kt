package com.meridia.shared.viewModels

import com.meridia.shared.models.BoxDto
import com.meridia.shared.models.BoxItemDto
import com.meridia.shared.models.MealDto
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
import kotlin.test.assertTrue

class BoxViewModelTest {

    private val plan = PlanDto(id = 1, name = "Dimagrimento 1400 kcal", dailyKcal = 1400, weeks = 4)

    private fun meal(id: Int, slot: String) =
        MealDto(id = id, title = "Pasto $id", slot = slot, kcal = 500, proteinG = 30, carbG = 40, fatG = 15)

    private val box = BoxDto(
        weekIndex = 0,
        items = listOf(
            BoxItemDto(weekday = 0, slot = "lunch", meal = meal(1, "lunch")),
            BoxItemDto(weekday = 0, slot = "dinner", meal = meal(2, "dinner")),
        ),
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
    fun no_plan_emits_locked() = runTest {
        val vm = BoxViewModel(FakeBoxRepository(plan = null))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is BoxUiState.Locked)
    }

    @Test
    fun plan_and_box_emit_content() = runTest {
        val vm = BoxViewModel(FakeBoxRepository(plan = plan, box = box))
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is BoxUiState.Content)
        assertEquals("Dimagrimento 1400 kcal", (state as BoxUiState.Content).plan.name)
        assertEquals(2, state.box.items.size)
    }

    @Test
    fun plan_failure_emits_error() = runTest {
        val vm = BoxViewModel(FakeBoxRepository(planError = BookingException("Errore di rete")))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is BoxUiState.Error)
    }

    @Test
    fun select_weekday_updates_content() = runTest {
        val vm = BoxViewModel(FakeBoxRepository(plan = plan, box = box))
        vm.load()
        advanceUntilIdle()
        vm.selectWeekday(3)
        val state = vm.state.value
        assertTrue(state is BoxUiState.Content)
        assertEquals(3, (state as BoxUiState.Content).selectedWeekday)
    }
}

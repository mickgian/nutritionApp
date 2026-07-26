package com.meridia.shared.viewModels

import com.meridia.shared.models.MealDto
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

class MealDetailViewModelTest {

    private val meal = MealDto(
        id = 7,
        title = "Pollo alle erbe con quinoa",
        slot = "lunch",
        kcal = 520,
        proteinG = 42,
        carbG = 48,
        fatG = 16,
        assetRef = "🍗",
        storageNote = "Conservare in frigorifero.",
        reheatNote = "Riscaldare 2-3 minuti.",
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
        val vm = MealDetailViewModel(FakeMealRepository(result = meal))
        vm.load(7)
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is MealDetailUiState.Content)
        assertEquals("Pollo alle erbe con quinoa", (state as MealDetailUiState.Content).meal.title)
        assertEquals(520, state.meal.kcal)
    }

    @Test
    fun load_failure_emits_italian_error() = runTest {
        val vm = MealDetailViewModel(
            FakeMealRepository(error = BookingException("Pasto non trovato")),
        )
        vm.load(999)
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is MealDetailUiState.Error)
        assertEquals("Pasto non trovato", (state as MealDetailUiState.Error).message)
    }
}

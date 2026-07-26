package com.meridia.shared.viewModels

import com.meridia.shared.models.ProfessionalDto
import com.meridia.shared.models.ReviewDto
import com.meridia.shared.models.UserResponse
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

class ConsulenzaViewModelTest {

    private val review = ReviewDto(
        id = 1,
        authorName = "Giulia M.",
        rating = 5,
        body = "Ottimo percorso.",
        createdAt = "2026-07-01T10:00:00",
    )
    private val professional = ProfessionalDto(
        id = 1,
        fullName = "Dott.ssa Anna Serra",
        title = "Biologa Nutrizionista",
        bio = "Bio.",
        rating = 4.75,
        reviewsCount = 1,
        reviews = listOf(review),
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
    fun load_success_emits_content_with_reviews() = runTest {
        val vm = ConsulenzaViewModel(FakeProfessionalRepository(result = professional))
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is ConsulenzaUiState.Content)
        assertEquals("Dott.ssa Anna Serra", (state as ConsulenzaUiState.Content).professional.fullName)
        assertEquals(1, state.professional.reviews.size)
    }

    @Test
    fun load_success_with_no_reviews_still_content() = runTest {
        val vm = ConsulenzaViewModel(
            FakeProfessionalRepository(
                result = professional.copy(reviewsCount = 0, reviews = emptyList()),
            ),
        )
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is ConsulenzaUiState.Content)
        assertTrue((state as ConsulenzaUiState.Content).professional.reviews.isEmpty())
    }

    @Test
    fun load_failure_emits_italian_error() = runTest {
        val vm = ConsulenzaViewModel(
            FakeProfessionalRepository(error = BookingException("Impossibile caricare il profilo. Riprova.")),
        )
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is ConsulenzaUiState.Error)
        assertEquals("Impossibile caricare il profilo. Riprova.", (state as ConsulenzaUiState.Error).message)
    }

    @Test
    fun admin_user_content_exposes_is_admin_true() = runTest {
        val vm = ConsulenzaViewModel(
            FakeProfessionalRepository(result = professional),
            FakeAuthRepository(registerResult = UserResponse(1, "studio@meridia.it", "Studio", "admin")),
        )
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is ConsulenzaUiState.Content)
        assertTrue((state as ConsulenzaUiState.Content).isAdmin)
    }

    @Test
    fun cliente_user_content_is_not_admin() = runTest {
        val vm = ConsulenzaViewModel(
            FakeProfessionalRepository(result = professional),
            FakeAuthRepository(registerResult = UserResponse(2, "mario@example.com", "Mario", "cliente")),
        )
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is ConsulenzaUiState.Content)
        assertEquals(false, (state as ConsulenzaUiState.Content).isAdmin)
    }
}

package com.meridia.shared.viewModels

import com.meridia.shared.models.UserResponse
import com.meridia.shared.network.auth.AuthException
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

class RegistrationViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun register_success_emits_success_and_forwards_full_name() = runTest {
        val fake = FakeAuthRepository(
            registerResult = UserResponse(7, "mario@example.com", "Mario Rossi", "cliente"),
        )
        val vm = RegistrationViewModel(fake)

        vm.register("mario@example.com", "Password1!", "Mario Rossi")
        advanceUntilIdle()

        assertTrue(vm.state.value is RegistrationViewModel.State.Success)
        assertEquals("Mario Rossi", fake.lastRegisterFullName)
    }

    @Test
    fun register_failure_emits_italian_error_state() = runTest {
        val vm = RegistrationViewModel(FakeAuthRepository(registerError = AuthException("Email già registrata")))

        vm.register("mario@example.com", "Password1!", "Mario Rossi")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is RegistrationViewModel.State.Error)
        assertEquals("Email già registrata", (state as RegistrationViewModel.State.Error).message)
    }
}

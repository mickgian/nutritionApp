package com.meridia.shared.viewModels

import com.meridia.shared.models.TokenResponse
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

class LoginViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_success_emits_success_state() = runTest {
        val token = TokenResponse(accessToken = "abc", tokenType = "bearer")
        val vm = LoginViewModel(FakeAuthRepository(loginResult = token))

        vm.login("mario@example.com", "password123")
        advanceUntilIdle()

        val state = vm.loginState.value
        assertTrue(state is LoginViewModel.LoginState.Success)
        assertEquals("abc", (state as LoginViewModel.LoginState.Success).token.accessToken)
    }

    @Test
    fun login_failure_emits_italian_error_state() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(loginError = AuthException("Credenziali non valide")))

        vm.login("mario@example.com", "wrong-pass")
        advanceUntilIdle()

        val state = vm.loginState.value
        assertTrue(state is LoginViewModel.LoginState.Error)
        assertEquals("Credenziali non valide", (state as LoginViewModel.LoginState.Error).message)
    }
}

package com.meridia.shared.viewModels

import com.meridia.shared.models.AdminClientDto
import com.meridia.shared.models.AdminOrderDto
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

class AdminViewModelTest {

    private val clients = listOf(
        AdminClientDto(id = 1, fullName = "Mario Rossi", email = "mario@example.com", activePlan = null),
        AdminClientDto(id = 2, fullName = "Giulia Bianchi", email = "giulia@example.com", activePlan = "Sportivo"),
    )
    private val orders = listOf(
        AdminOrderDto(1, 1, "Mario Rossi", "mario@example.com", "single", "mattina", "paid", 8900, "2026-07-26T10:00:00Z"),
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
    fun load_success_emits_content_with_clients_and_orders() = runTest {
        val vm = AdminViewModel(FakeAdminRepository(clients = clients, orders = orders))
        vm.load()
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is AdminUiState.Content)
        assertEquals(2, (state as AdminUiState.Content).clients.size)
        assertEquals(1, state.orders.size)
    }

    @Test
    fun load_failure_emits_error() = runTest {
        val vm = AdminViewModel(FakeAdminRepository(loadError = BookingException("Accesso riservato allo studio")))
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.state.value is AdminUiState.Error)
    }

    @Test
    fun assign_plan_reloads_and_reflects_new_active_plan() = runTest {
        val repo = FakeAdminRepository(clients = clients, orders = orders)
        val vm = AdminViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.assignPlan(clientId = 1, name = "Dimagrimento 1400 kcal", dailyKcal = 1400, weeks = 4)
        advanceUntilIdle()
        val content = vm.state.value as AdminUiState.Content
        assertEquals(1, repo.lastAssignedClientId)
        assertEquals("Dimagrimento 1400 kcal", repo.lastAssignedPlan)
        assertEquals("Dimagrimento 1400 kcal", content.clients.first { it.id == 1 }.activePlan)
        assertNull(content.assigningClientId)
    }

    @Test
    fun assign_plan_failure_sets_action_error() = runTest {
        val vm = AdminViewModel(
            FakeAdminRepository(clients = clients, orders = orders, assignError = BookingException("Cliente non trovato")),
        )
        vm.load()
        advanceUntilIdle()
        vm.assignPlan(clientId = 99, name = "Piano", dailyKcal = 1400, weeks = 4)
        advanceUntilIdle()
        val content = vm.state.value as AdminUiState.Content
        assertEquals("Cliente non trovato", content.actionError)
        assertNull(content.assigningClientId)
    }

    @Test
    fun send_promotion_success_sets_italian_notice() = runTest {
        val repo = FakeAdminRepository(clients = clients, orders = orders, promotionRecipients = 43)
        val vm = AdminViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.sendPromotion("Promo di settembre", "Porta un amico")
        advanceUntilIdle()
        val content = vm.state.value as AdminUiState.Content
        assertEquals("Promo di settembre", repo.lastPromotionTitle)
        assertEquals("Promozione inviata a 43 clienti", content.promoNotice)
        assertEquals(false, content.promoSubmitting)
    }

    @Test
    fun send_promotion_singular_recipient_uses_singular_word() = runTest {
        val vm = AdminViewModel(FakeAdminRepository(clients = clients, orders = orders, promotionRecipients = 1))
        vm.load()
        advanceUntilIdle()
        vm.sendPromotion("Promo", "Sconto")
        advanceUntilIdle()
        val content = vm.state.value as AdminUiState.Content
        assertEquals("Promozione inviata a 1 cliente", content.promoNotice)
    }

    @Test
    fun send_promotion_blank_input_sets_error_without_calling_repo() = runTest {
        val repo = FakeAdminRepository(clients = clients, orders = orders)
        val vm = AdminViewModel(repo)
        vm.load()
        advanceUntilIdle()
        vm.sendPromotion("   ", "")
        advanceUntilIdle()
        val content = vm.state.value as AdminUiState.Content
        assertTrue(content.actionError != null)
        assertNull(repo.lastPromotionTitle)
    }

    @Test
    fun send_promotion_failure_sets_action_error() = runTest {
        val vm = AdminViewModel(
            FakeAdminRepository(
                clients = clients,
                orders = orders,
                promotionError = BookingException("Operazione non riuscita. Riprova."),
            ),
        )
        vm.load()
        advanceUntilIdle()
        vm.sendPromotion("Promo", "Sconto")
        advanceUntilIdle()
        val content = vm.state.value as AdminUiState.Content
        assertEquals("Operazione non riuscita. Riprova.", content.actionError)
        assertEquals(false, content.promoSubmitting)
    }
}

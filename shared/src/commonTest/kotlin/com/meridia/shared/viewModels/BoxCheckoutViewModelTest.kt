package com.meridia.shared.viewModels

import com.meridia.shared.models.OrderDto
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

class BoxCheckoutViewModelTest {

    private val placed = OrderDto(
        id = 5,
        formula = "single",
        pickup = "pomeriggio",
        status = "pending_payment",
        priceCents = 8900,
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
    fun defaults_to_subscription_and_morning_pickup() {
        val vm = BoxCheckoutViewModel(FakeOrderRepository())
        val state = vm.state.value
        assertEquals(OrderFormula.Subscription, state.formula)
        assertEquals(PickupSlot.Mattina, state.pickup)
        assertNull(state.placedOrder)
    }

    @Test
    fun selecting_formula_and_pickup_updates_state() {
        val vm = BoxCheckoutViewModel(FakeOrderRepository())
        vm.selectFormula(OrderFormula.Single)
        vm.selectPickup(PickupSlot.Pomeriggio)
        assertEquals(OrderFormula.Single, vm.state.value.formula)
        assertEquals(PickupSlot.Pomeriggio, vm.state.value.pickup)
    }

    @Test
    fun place_success_sets_placed_order_and_sends_wire_values() = runTest {
        val repo = FakeOrderRepository(createResult = placed)
        val vm = BoxCheckoutViewModel(repo)
        vm.selectFormula(OrderFormula.Single)
        vm.selectPickup(PickupSlot.Pomeriggio)
        vm.place()
        advanceUntilIdle()
        assertEquals(placed, vm.state.value.placedOrder)
        assertEquals(false, vm.state.value.submitting)
        assertEquals("single", repo.lastFormula)
        assertEquals("pomeriggio", repo.lastPickup)
    }

    @Test
    fun place_failure_sets_italian_error() = runTest {
        val vm = BoxCheckoutViewModel(
            FakeOrderRepository(createError = BookingException("Serve prima un piano")),
        )
        vm.place()
        advanceUntilIdle()
        assertEquals("Serve prima un piano", vm.state.value.error)
        assertEquals(false, vm.state.value.submitting)
        assertNull(vm.state.value.placedOrder)
    }

    @Test
    fun place_is_ignored_once_order_is_placed() = runTest {
        val repo = FakeOrderRepository(createResult = placed)
        val vm = BoxCheckoutViewModel(repo)
        vm.place()
        advanceUntilIdle()
        assertTrue(vm.state.value.placedOrder != null)
        // A second tap must not re-submit.
        vm.selectFormula(OrderFormula.Subscription)
        vm.place()
        advanceUntilIdle()
        assertEquals("single", repo.lastFormula) // unchanged from the first successful call
    }
}

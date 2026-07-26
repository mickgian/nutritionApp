package com.meridia.shared.viewModels

import com.meridia.shared.models.OrderDto
import com.meridia.shared.network.OrderRepository

/** In-memory [OrderRepository] for box checkout / box ViewModel tests. */
class FakeOrderRepository(
    private val orders: List<OrderDto> = emptyList(),
    private val createResult: OrderDto? = null,
    private val createError: Throwable? = null,
    private val listError: Throwable? = null,
) : OrderRepository {

    var lastFormula: String? = null
        private set
    var lastPickup: String? = null
        private set

    override suspend fun createOrder(formula: String, pickup: String): OrderDto {
        lastFormula = formula
        lastPickup = pickup
        createError?.let { throw it }
        return createResult ?: error("No createResult configured")
    }

    override suspend fun myOrders(): List<OrderDto> {
        listError?.let { throw it }
        return orders
    }
}

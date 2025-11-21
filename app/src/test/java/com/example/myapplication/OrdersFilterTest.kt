package com.example.myapplication

import com.example.myapplication.model.Order
import com.example.myapplication.ui.OrdersFragment
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersFilterTest {
    @Test
    fun sortByDateDesc() {
        val a = Order(1, "pendiente", 0.0, emptyList(), 0, 1000L, 0)
        val b = Order(2, "pendiente", 0.0, emptyList(), 0, 2000L, 0)
        val res = OrdersFragment.sortOrders(listOf(a, b), "fecha_desc")
        assertEquals(2, res.first().id)
    }

    @Test
    fun filterByStatusAndDate() {
        val a = Order(1, "pendiente", 0.0, emptyList(), 0, 1000L, 0)
        val b = Order(2, "confirmada", 0.0, emptyList(), 0, 2000L, 0)
        val res = OrdersFragment.applyClientFilters(listOf(a, b), "pendiente", 500L, 1500L, null)
        assertEquals(1, res.size)
        assertEquals(1, res.first().id)
    }
}
package com.example.myapplication.ui

import com.example.myapplication.model.Order
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersFragmentTest {
    @Test
    fun filterByUserOrDiscount_returns_only_matching_user() {
        val list = listOf(
            Order(1, "pendiente", 10.0, emptyList(), 5, 1000L, null),
            Order(2, "pendiente", 20.0, emptyList(), 7, 1001L, null),
            Order(3, "cancelada", 30.0, emptyList(), 5, 1002L, null)
        )
        val filtered = OrdersFragment.filterByUserOrDiscount(list, 5)
        assertEquals(2, filtered.size)
        assertEquals(listOf(1,3), filtered.map { it.id })
    }

    @Test
    fun applyClientFilters_excludes_invalid_status_when_onlyValid_true() {
        val list = listOf(
            Order(1, "pendiente", 10.0, emptyList(), 5, 1000L, null),
            Order(2, "rechazado", 20.0, emptyList(), 5, 1001L, null),
            Order(3, "cancelada", 30.0, emptyList(), 5, 1002L, null),
            Order(4, "completada", 40.0, emptyList(), 5, 1003L, null)
        )
        val filtered = OrdersFragment.applyClientFilters(list, null, null, null, null, null, null, true)
        assertEquals(listOf(1,4), filtered.map { it.id })
    }

    @Test
    fun slicePage_returns_expected_elements() {
        val list = (1..25).map { Order(it, "pendiente", it.toDouble(), emptyList(), 1, 1000L + it, null) }
        val page1 = OrdersFragment.slicePage(list, 1, 10)
        val page3 = OrdersFragment.slicePage(list, 3, 10)
        assertEquals((1..10).toList(), page1.map { it.id })
        assertEquals((21..25).toList(), page3.map { it.id })
    }
}
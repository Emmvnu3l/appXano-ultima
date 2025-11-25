package com.example.myapplication.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OrdersActionsTest {
    @Test
    fun actions_for_pendiente() {
        val (proc, complete, cancel) = OrdersAdapter.actionsForStatus("pendiente")
        assertEquals(true, proc)
        assertEquals(false, complete)
        assertEquals(true, cancel)
    }

    @Test
    fun actions_for_en_proceso() {
        val (proc, complete, cancel) = OrdersAdapter.actionsForStatus("en_proceso")
        assertEquals(false, proc)
        assertEquals(true, complete)
        assertEquals(true, cancel)
    }

    @Test
    fun actions_for_completada() {
        val (proc, complete, cancel) = OrdersAdapter.actionsForStatus("completada")
        assertEquals(false, proc)
        assertEquals(false, complete)
        assertEquals(false, cancel)
    }
}

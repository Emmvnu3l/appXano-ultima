package com.example.myapplication.ui

import com.example.myapplication.model.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UsersAdapterTest {
    @Test
    fun computeStatus_blocked_by_flag() {
        val u = User(1, "n", "e", null, null, true, null, null, null, "active", null, null, null)
        val (blocked, label) = UsersAdapter.computeStatus(u)
        assertEquals(true, blocked)
        assertEquals("bloqueado", label)
    }

    @Test
    fun computeStatus_blocked_by_status() {
        val u = User(2, "n", "e", null, null, false, null, null, null, "blocked", null, null, null)
        val (blocked, label) = UsersAdapter.computeStatus(u)
        assertEquals(true, blocked)
        assertEquals("bloqueado", label)
    }

    @Test
    fun computeStatus_active() {
        val u = User(3, "n", "e", null, null, false, null, null, null, "active", null, null, null)
        val (blocked, label) = UsersAdapter.computeStatus(u)
        assertEquals(false, blocked)
        assertEquals("activo", label)
    }

    @Test
    fun computeStatus_disconnected() {
        val u = User(4, "n", "e", null, null, false, null, null, null, "disconnected", null, null, null)
        val (blocked, label) = UsersAdapter.computeStatus(u)
        assertEquals(false, blocked)
        assertEquals("desconectado", label)
    }
}

package com.example.myapplication.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UsersFragmentUtilsTest {
    @Test
    fun computeMaxPage_basic() {
        assertEquals(1, UsersFragment.computeMaxPage(0, 10))
        assertEquals(1, UsersFragment.computeMaxPage(5, 10))
        assertEquals(2, UsersFragment.computeMaxPage(11, 10))
        assertEquals(3, UsersFragment.computeMaxPage(21, 10))
    }

    @Test
    fun computeMaxPage_invalidPerPage() {
        assertEquals(1, UsersFragment.computeMaxPage(100, 0))
        assertEquals(1, UsersFragment.computeMaxPage(100, -5))
    }

    // filtros eliminados, solo se prueba paginación
}

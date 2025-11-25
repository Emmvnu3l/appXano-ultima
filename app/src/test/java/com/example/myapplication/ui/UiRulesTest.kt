package com.example.myapplication.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiRulesTest {
    @Test
    fun fabMode_rules_are_correct() {
        assertEquals("profile", HomeActivity.fabModeForFragment(ProfileFragment()))
        assertEquals("cart", HomeActivity.fabModeForFragment(CategoriesFragment()))
        assertEquals("cart", HomeActivity.fabModeForFragment(ProductsFragment.newInstance(null)))
        assertEquals("hidden", HomeActivity.fabModeForFragment(OrdersFragment()))
    }
}

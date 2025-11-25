package com.example.myapplication.ui

import com.example.myapplication.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductsFilterTest {
    private fun p(id: Int, name: String, cat: Int?, desc: String? = null): Product {
        return Product(id = id, name = name, description = desc, price = 1.0, stock = 1, brand = null, categoryId = cat, img = emptyList<com.example.myapplication.model.ProductImage>())
    }

    @Test
    fun exact_category_name_matches_products_in_category() {
        val list = listOf(
            p(1, "Gorro", 10),
            p(2, "Calabaza grande", 20),
            p(3, "Disfraz Halloween", 20)
        )
        val names = mapOf(10 to "Accesorios", 20 to "Halloween")
        val filtered = ProductsFragment.filterProducts(list, "Halloween", null, names)
        assertEquals(listOf(2,3), filtered.map { it.id })
    }

    @Test
    fun search_token_matches_in_name_description_and_category() {
        val list = listOf(
            p(1, "Taza temática", 30, "Edición especial Halloween"),
            p(2, "Calabaza", 20),
            p(3, "Sombrero", 10)
        )
        val names = mapOf(10 to "Accesorios", 20 to "Halloween", 30 to "Hogar")
        val filtered1 = ProductsFragment.filterProducts(list, "halloween", null, names)
        val filtered2 = ProductsFragment.filterProducts(list, "hogar calabaza", null, names)
        assertEquals(listOf(1,2), filtered1.map { it.id })
        assertEquals(listOf(1,2), filtered2.map { it.id })
    }

    @Test
    fun handles_accents_and_case_and_special_chars() {
        val list = listOf(
            p(1, "DISFRAZ HALLOWEEN", 20),
            p(2, "Disfraz hallowe'en niño", 20)
        )
        val names = mapOf(20 to "Hálloweén")
        val filtered1 = ProductsFragment.filterProducts(list, "hÁllowéen", null, names)
        val filtered2 = ProductsFragment.filterProducts(list, "hallowe'en", null, names)
        assertEquals(listOf(1,2), filtered1.map { it.id })
        assertEquals(listOf(1,2), filtered2.map { it.id })
    }

    @Test
    fun category_filter_combines_with_query() {
        val list = listOf(
            p(1, "Calabaza pequeña", 20),
            p(2, "Calabaza grande", 20),
            p(3, "Sombrero", 10)
        )
        val names = mapOf(20 to "Halloween", 10 to "Accesorios")
        val filtered = ProductsFragment.filterProducts(list, "grande", 20, names)
        assertEquals(listOf(2), filtered.map { it.id })
    }

    @Test
    fun non_existing_category_name_returns_empty_without_errors() {
        val list = listOf(p(1, "Gorro", 10))
        val names = mapOf(10 to "Accesorios")
        val filtered = ProductsFragment.filterProducts(list, "NoExiste", null, names)
        assertEquals(0, filtered.size)
    }
}

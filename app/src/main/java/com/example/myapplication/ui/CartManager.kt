package com.example.myapplication.ui

import android.content.Context
import android.content.SharedPreferences

class CartManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(productId: Int, quantity: Int) {
        if (quantity <= 0) return
        val map = getItems().toMutableMap()
        val current = map[productId] ?: 0
        map[productId] = current + quantity
        save(map)
    }

    fun update(productId: Int, quantity: Int) {
        val map = getItems().toMutableMap()
        if (quantity <= 0) {
            map.remove(productId)
        } else {
            map[productId] = quantity
        }
        save(map)
    }

    fun remove(productId: Int) {
        val map = getItems().toMutableMap()
        map.remove(productId)
        save(map)
    }

    fun clear() {
        prefs.edit().remove(KEY_CART).apply()
    }

    fun getItems(): Map<Int, Int> {
        val raw = prefs.getString(KEY_CART, null) ?: return emptyMap()
        val map = mutableMapOf<Int, Int>()
        if (raw.isNotEmpty()) {
            val pairs = raw.split(';')
            for (p in pairs) {
                val parts = p.split('=')
                if (parts.size == 2) {
                    val id = parts[0].toIntOrNull()
                    val qty = parts[1].toIntOrNull()
                    if (id != null && qty != null) map[id] = qty
                }
            }
        }
        return map
    }

    private fun save(items: Map<Int, Int>) {
        val serialized = items.entries.joinToString(";") { "${it.key}=${it.value}" }
        prefs.edit().putString(KEY_CART, serialized).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "cart_prefs"
        private const val KEY_CART = "cart_items"
    }
}
package com.example.myapplication.ui

import android.content.Context
import android.content.Intent
import com.example.myapplication.R

object NavigationHelper {
    fun openAddProduct(context: Context) {
        val tm = com.example.myapplication.api.TokenManager(context)
        if (!tm.isAdmin()) return
        if (context is HomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddProductFragment())
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_add_product", true)
            context.startActivity(intent)
        }
    }

    fun openCreateCategory(context: Context) {
        val tm = com.example.myapplication.api.TokenManager(context)
        if (!tm.isAdmin()) return
        if (context is HomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CreateCategoryFragment())
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_create_category", true)
            context.startActivity(intent)
        }
    }

    fun openCart(context: Context) {
        if (context is HomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CartFragment())
                .commit()
        } else if (context is LimitedHomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CartFragment())
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_products", true)
            context.startActivity(intent)
        }
    }

    fun openCheckout(context: Context) {
        context.startActivity(Intent(context, CheckoutActivity::class.java))
    }

    fun openOrders(context: Context) {
        if (context is HomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OrdersFragment())
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_orders", true)
            context.startActivity(intent)
        }
    }

    fun openUsers(context: Context) {
        if (context is HomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, UsersFragment())
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_users", true)
            context.startActivity(intent)
        }
    }
}
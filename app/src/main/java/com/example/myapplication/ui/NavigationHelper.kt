package com.example.myapplication.ui

import android.content.Context
import android.content.Intent
import com.example.myapplication.R

object NavigationHelper {
    fun openAddProduct(context: Context) {
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
}
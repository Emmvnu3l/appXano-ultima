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
                .setCustomAnimations(
                    R.anim.fade_in_300,
                    R.anim.fade_out_300,
                    R.anim.fade_in_300,
                    R.anim.fade_out_300
                )
                .replace(R.id.fragmentContainer, CartFragment())
                .commit()
        } else if (context is LimitedHomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in_300,
                    R.anim.fade_out_300,
                    R.anim.fade_in_300,
                    R.anim.fade_out_300
                )
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

    fun openOrders(context: Context, limit: Int? = null) {
        if (context is HomeActivity) {
            val frag = OrdersFragment()
            val args = android.os.Bundle()
            if (limit != null) args.putInt("limit", limit)
            frag.arguments = args
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_orders", true)
            if (limit != null) intent.putExtra("orders_limit", limit)
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

    fun openEditProduct(context: Context, product: com.example.myapplication.model.Product) {
        if (context is HomeActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditProductFragment.newInstance(product))
                .commit()
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_edit_product", true)
            intent.putExtra("product", product)
            context.startActivity(intent)
        }
    }

    fun setupCartFab(activity: androidx.appcompat.app.AppCompatActivity, fab: com.google.android.material.floatingactionbutton.FloatingActionButton) {
        fab.setOnClickListener { openCart(activity) }
        val cm = CartManager(activity)
        var badge = fab.getTag(com.example.myapplication.R.id.tag_cart_badge) as? com.google.android.material.badge.BadgeDrawable
        if (badge == null) {
            badge = com.google.android.material.badge.BadgeDrawable.create(activity)
            badge.badgeGravity = com.google.android.material.badge.BadgeDrawable.BOTTOM_END
            badge.verticalOffset = 6
            badge.horizontalOffset = 6
            fab.setTag(com.example.myapplication.R.id.tag_cart_badge, badge)
            try {
                com.google.android.material.badge.BadgeUtils.attachBadgeDrawable(badge, fab)
            } catch (_: Throwable) { }
        }

        fun updateBadge() {
            val count = cm.getItems().values.sum()
            if (count > 0) {
                badge?.isVisible = true
                badge?.number = count
                fab.contentDescription = "Abrir carrito, $count artículos"
            } else {
                badge?.clearNumber()
                badge?.isVisible = false
                fab.contentDescription = "Abrir carrito"
            }
        }

        updateBadge()

        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> updateBadge() }
        fab.setTag(com.example.myapplication.R.id.tag_cart_prefs_listener, listener)
        cm.registerListener(listener)

        fab.alpha = 0f
        fab.scaleX = 0.9f
        fab.scaleY = 0.9f
        fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }
}

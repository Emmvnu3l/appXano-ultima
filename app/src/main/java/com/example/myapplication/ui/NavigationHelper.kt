package com.example.myapplication.ui

import android.content.Context
import android.content.Intent
import com.example.myapplication.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, CreateCategoryFragment())
                    .commit()
            }
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_create_category", true)
            context.startActivity(intent)
        }
    }

    fun openCart(context: Context) {
        if (context is HomeActivity) {
            val toolbarCart = context.findViewById<android.view.View>(R.id.actionCart)
            toolbarCart?.visibility = android.view.View.GONE
            val fab = context.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabProfile)
            fab?.let {
                it.visibility = android.view.View.GONE
                val badge = it.getTag(R.id.tag_cart_badge) as? com.google.android.material.badge.BadgeDrawable
                badge?.isVisible = false
                val listener = it.getTag(R.id.tag_cart_prefs_listener) as? android.content.SharedPreferences.OnSharedPreferenceChangeListener
                if (listener != null) {
                    try { CartManager(context).unregisterListener(listener) } catch (_: Exception) {}
                }
                it.setOnClickListener(null)
            }
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, CartFragment())
                    .commit()
            }
        } else if (context is LimitedHomeActivity) {
            val toolbarCart = context.findViewById<android.view.View>(R.id.actionCart)
            toolbarCart?.visibility = android.view.View.GONE
            val fab = context.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabProfile)
            fab?.let {
                it.visibility = android.view.View.GONE
                val badge = it.getTag(R.id.tag_cart_badge) as? com.google.android.material.badge.BadgeDrawable
                badge?.isVisible = false
                val listener = it.getTag(R.id.tag_cart_prefs_listener) as? android.content.SharedPreferences.OnSharedPreferenceChangeListener
                if (listener != null) {
                    try { CartManager(context).unregisterListener(listener) } catch (_: Exception) {}
                }
                it.setOnClickListener(null)
            }
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, CartFragment())
                    .commit()
            }
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_products", true)
            context.startActivity(intent)
        }
    }

    fun openCheckout(context: Context) {
        if (context is HomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, CheckoutFragment())
                    .addToBackStack(null)
                    .commit()
            }
        } else if (context is LimitedHomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, CheckoutFragment())
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            context.startActivity(Intent(context, CheckoutActivity::class.java))
        }
    }

    fun openProductDetails(context: Context, product: com.example.myapplication.model.Product) {
        openProductDetail(context, product)
    }

    fun openProductDetail(context: Context, product: com.example.myapplication.model.Product) {
        if (context is HomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, ProductDetailFragment.newInstance(product))
                    .addToBackStack(null)
                    .commit()
            }
        } else if (context is LimitedHomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, ProductDetailFragment.newInstance(product))
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            val intent = Intent(context, ProductDetailActivity::class.java)
            intent.putExtra("product", product)
            context.startActivity(intent)
        }
    }

    fun openProfileDetails(context: Context) {
        if (context is HomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, ProfileDetailsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        } else if (context is LimitedHomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, ProfileDetailsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_profile_details", true)
            context.startActivity(intent)
        }
    }

    fun openOrders(context: Context, limit: Int? = null) {
        if (context is HomeActivity) {
            val frag = OrdersFragment()
            val args = android.os.Bundle()
            if (limit != null) args.putInt("limit", limit)
            frag.arguments = args
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, frag)
                    .commit()
            }
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_orders", true)
            if (limit != null) intent.putExtra("orders_limit", limit)
            context.startActivity(intent)
        }
    }

    fun openUsers(context: Context) {
        if (context is HomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, UsersFragment())
                    .commit()
            }
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_users", true)
            context.startActivity(intent)
        }
    }
   fun openCategories(context: Context) {
        if (context is HomeActivity) {
            context.showOverlayLoading()
            context.lifecycleScope.launch {
                try {
                    val service = com.example.myapplication.api.RetrofitClient.createCategoryService(context)
                    val list = withContext(kotlinx.coroutines.Dispatchers.IO) { service.getCategories() }
                    val frag = CategoriesFragment()
                    val args = android.os.Bundle()
                    args.putSerializable("preloaded_categories", java.util.ArrayList(list))
                    frag.arguments = args
                    context.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, frag)
                        .commit()
                    context.hideOverlay()
                } catch (e: Exception) {
                    context.showOverlayError(com.example.myapplication.api.NetworkError.message(e))
                    // Opcional: fallback sin prefetch
                    context.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CategoriesFragment())
                        .commit()
                } finally {
                    // En caso de fallback, ocultar overlay después de que el fragmento muestre su propio loader
                }
            }
        } else {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra("open_categories", true)
            context.startActivity(intent)
        }
    }
    fun openEditProduct(context: Context, product: com.example.myapplication.model.Product) {
        if (context is HomeActivity) {
            if (!context.isFinishing && !context.isDestroyed && !context.supportFragmentManager.isStateSaved) {
                try {
                    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    val view = context.currentFocus ?: context.findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                } catch (_: Exception) {}
                context.supportFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, EditProductFragment.newInstance(product))
                    .commit()
            }
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

    fun setupProfileFab(activity: androidx.appcompat.app.AppCompatActivity, fab: com.google.android.material.floatingactionbutton.FloatingActionButton) {
        fab.setOnClickListener { openProfileDetails(activity) }
        fab.alpha = 0f
        fab.scaleX = 0.9f
        fab.scaleY = 0.9f
        fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }
}

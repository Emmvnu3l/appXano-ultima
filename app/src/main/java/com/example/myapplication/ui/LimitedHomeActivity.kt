package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityLimitedHomeBinding

class LimitedHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLimitedHomeBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        try {
            binding = ActivityLimitedHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (t: Throwable) {
            android.util.Log.e("LimitedHomeActivity", "init failed", t)
            val tv = android.widget.TextView(this).apply {
                text = t.localizedMessage ?: "Error"
                setBackgroundColor(android.graphics.Color.WHITE)
                setTextColor(android.graphics.Color.RED)
                textSize = 16f
                gravity = android.view.Gravity.CENTER
            }
            setContentView(tv)
            return
        }

        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        try {
            val header = binding.navView.getHeaderView(0)
            val tv = header.findViewById<android.widget.TextView>(R.id.tvHeaderSubtitle)
            val name = com.example.myapplication.api.TokenManager(this).getName()
            tv?.text = if (!name.isNullOrBlank()) "Bienvenido ${name}" else "Bienvenido"
        } catch (_: Exception) {}

        binding.toolbar.findViewById<View>(R.id.actionCart)?.setOnClickListener {
            NavigationHelper.openCart(this)
        }

        // Estado del FAB se ajusta según el fragmento activo

        replaceFragment(ProductsFragment.newInstance(null))
        binding.navView.setCheckedItem(R.id.nav_products)

        binding.navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_products -> replaceFragment(ProductsFragment.newInstance(null))
                R.id.nav_cart -> { NavigationHelper.openCart(this); true }
                R.id.nav_orders -> replaceFragment(OrdersFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_logout -> {
                    startActivity(Intent(this, LogoutActivity::class.java))
                    true
                }
                else -> false
            }
            if (handled) {
                binding.navView.setCheckedItem(item.itemId)
                binding.drawerLayout.closeDrawers()
            }
            handled
        }

        val tm = TokenManager(this)
        if (tm.isAdmin()) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        if (isFinishing || isDestroyed) return false
        if (supportFragmentManager.isStateSaved) return false
        hideKeyboard()
        
        // Clear back stack to avoid deep nesting when switching main tabs
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        return try {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(binding.fragmentContainer.id, fragment)
                .commit()
            configureFabForFragment(fragment)
            logMem("replaceFragment")
            true
        } catch (t: Throwable) {
            android.util.Log.e("LimitedHomeActivity", "replaceFragment failed", t)
            false
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun configureFabForFragment(fragment: Fragment) {
        val fab = binding.fabProfile
        val actionCart = binding.toolbar.findViewById<View>(R.id.actionCart)
        clearFabBindings()
        when (fragment) {
            is ProfileFragment -> {
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_user)
                fab.contentDescription = "Abrir perfil"
                fab.setOnClickListener { NavigationHelper.openProfileDetails(this) }
                fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                actionCart?.visibility = View.VISIBLE
            }
            is CategoriesFragment -> {
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_cart)
                fab.contentDescription = "Abrir carrito"
                NavigationHelper.setupCartFab(this, fab)
                fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                actionCart?.visibility = View.VISIBLE
            }
            is ProductsFragment -> {
                fab.visibility = View.VISIBLE
                fab.setImageResource(R.drawable.ic_cart)
                fab.contentDescription = "Abrir carrito"
                NavigationHelper.setupCartFab(this, fab)
                fab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                actionCart?.visibility = View.VISIBLE
            }
            else -> {
                fab.visibility = View.GONE
                fab.setOnClickListener(null)
                actionCart?.visibility = if (fragment is CartFragment) View.GONE else View.VISIBLE
            }
        }
    }

    private fun clearFabBindings() {
        val fab = binding.fabProfile
        val badge = fab.getTag(R.id.tag_cart_badge) as? com.google.android.material.badge.BadgeDrawable
        badge?.isVisible = false
        val listener = fab.getTag(R.id.tag_cart_prefs_listener) as? android.content.SharedPreferences.OnSharedPreferenceChangeListener
        if (listener != null) {
            try {
                val cm = CartManager(this)
                cm.unregisterListener(listener)
            } catch (_: Exception) {}
            fab.setTag(R.id.tag_cart_prefs_listener, null)
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            val view = currentFocus ?: binding.root
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (_: Exception) {}
    }

    private fun logMem(source: String) {
        val rt = Runtime.getRuntime()
        val max = rt.maxMemory() / (1024 * 1024)
        val total = rt.totalMemory() / (1024 * 1024)
        val free = rt.freeMemory() / (1024 * 1024)
        android.util.Log.i("LimitedHomeActivity", "$source mem MB max=$max total=$total free=$free")
    }

    override fun onDestroy() {
        try { binding.drawerLayout.removeDrawerListener(toggle) } catch (_: Exception) {}
        clearFabBindings()
        super.onDestroy()
    }
}

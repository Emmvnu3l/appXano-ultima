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

        binding.toolbar.findViewById<View>(R.id.actionCart)?.setOnClickListener {
            NavigationHelper.openCart(this)
        }

        NavigationHelper.setupCartFab(this, binding.fabCart)

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
        // Ensure the container exists and clean up potential back stack issues
        if (supportFragmentManager.isStateSaved) return false
        
        // Clear back stack to avoid deep nesting when switching main tabs
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}

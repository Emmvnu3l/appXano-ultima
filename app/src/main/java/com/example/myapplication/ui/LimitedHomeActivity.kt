package com.example.myapplication.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
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
        binding = ActivityLimitedHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        replaceFragment(ProductsFragment.newInstance(null))
        binding.navView.setCheckedItem(R.id.nav_products)

        binding.navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_products -> replaceFragment(ProductsFragment.newInstance(null))
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_logout -> {
                    startActivity(android.content.Intent(this, LogoutActivity::class.java))
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
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
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
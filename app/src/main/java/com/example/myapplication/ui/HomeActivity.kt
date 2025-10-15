package com.example.myapplication.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar + Drawer toggle
        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Comentario: si venimos con un query de productos, abrimos la pestaña Productos filtrada.
        val productsQuery = intent?.getStringExtra("products_query")
        val openProducts = intent?.getBooleanExtra("open_products", false) ?: false
        if (openProducts || !productsQuery.isNullOrEmpty()) {
            replaceFragment(ProductsFragment.newInstance(productsQuery))
            binding.navView.setCheckedItem(R.id.nav_products)
        } else {
            // Fragment por defecto
            replaceFragment(HomeFragment())
            binding.navView.setCheckedItem(R.id.nav_home)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_products -> replaceFragment(ProductsFragment.newInstance(null))
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_add_product -> replaceFragment(AddProductFragment())
                else -> false
            }
            if (handled) {
                binding.navView.setCheckedItem(item.itemId)
                binding.drawerLayout.closeDrawers()
            }
            handled
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
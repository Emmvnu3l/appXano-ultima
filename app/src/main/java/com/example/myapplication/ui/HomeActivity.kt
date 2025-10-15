package com.example.myapplication.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        // Comentario: si venimos con un query de productos, abrimos la pestaña Productos filtrada.
        val productsQuery = intent?.getStringExtra("products_query")
        val openProducts = intent?.getBooleanExtra("open_products", false) ?: false
        if (openProducts || !productsQuery.isNullOrEmpty()) {
            replaceFragment(ProductsFragment.newInstance(productsQuery))
            binding.bottomNav.selectedItemId = R.id.nav_products
        } else {
            // Fragment por defecto
            replaceFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                // Comentario: al seleccionar Productos desde el bottom nav, sin filtro inicial.
                R.id.nav_products -> replaceFragment(ProductsFragment.newInstance(null))
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
        return true
    }
}
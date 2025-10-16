package com.example.myapplication.ui
// Activity para cerrar sesión. Usa Drawer + NavigationView con el mismo menú (@menu/nav_drawer_menu)
// para mantener consistencia visual. Marca el ítem 'Logout' como seleccionado.

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.myapplication.R
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityLogoutBinding

class LogoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar + Drawer
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Marcar elemento de menú actual
        binding.navView.setCheckedItem(R.id.nav_logout)

        // Navegación del drawer: enviamos a HomeActivity para otras secciones
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, HomeActivity::class.java).apply {
                        putExtra("open_products", true)
                    })
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add_product -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_logout -> {
                    // Nos mantenemos en esta pantalla
                    binding.drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        // Botón centrado para cerrar sesión
        binding.btnLogout.setOnClickListener {
            val tm = TokenManager(this)
            tm.clear()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
package com.example.myapplication.ui
// Activity para cerrar sesión. Usa Drawer + NavigationView con el mismo menú (@menu/nav_drawer_menu)
// para mantener consistencia visual. Marca el ítem 'Logout' como seleccionado.

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.api.TokenManager
import com.example.myapplication.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    navigateToHome(openProducts = false)
                    true
                }
                R.id.nav_products -> {
                    navigateToHome(openProducts = true)
                    true
                }
                R.id.nav_profile -> {
                    // Si vamos al perfil, idealmente deberíamos ir a HomeActivity y abrir el fragmento
                    // O simplemente reiniciar HomeActivity que por defecto (si es admin) puede manejarlo.
                    // Por simplicidad, vamos a Home
                    navigateToHome(openProducts = false)
                    true
                }
                // Eliminado nav_add_product ya que fue reemplazado y esta activity es genérica
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
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.createMembersServiceAuthenticated(this@LogoutActivity)
                            .updateStatus(mapOf("status" to "disconnected"))
                    }
                } catch (_: Exception) {}
                tm.clear()
                val intent = Intent(this@LogoutActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun navigateToHome(openProducts: Boolean) {
        val intent = Intent(this, HomeActivity::class.java)
        if (openProducts) {
            intent.putExtra("open_products", true)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

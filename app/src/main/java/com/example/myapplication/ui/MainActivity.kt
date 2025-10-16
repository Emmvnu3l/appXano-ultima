package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar sin flecha atrás y con título "Inicio"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
            title = "Inicio"
        }
        binding.toolbar.navigationIcon = null

        tokenManager = TokenManager(this)
        if (tokenManager.isLoggedIn()) {
            navigateToHome()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese email y password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doLogin(email, password)
        }

        // Navegar a la nueva actividad de Registro (Compose)
         binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin(email: String, password: String) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createAuthService(this@MainActivity)
                val response = withContext(Dispatchers.IO) {
                    service.login(LoginRequest(email, password))
                }
                val token = response.effectiveToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, "Token no recibido", Toast.LENGTH_SHORT).show()
                } else {
                    tokenManager.saveAuth(
                        token,
                        response.user?.name,
                        response.user?.email,
                        response.user?.id,
                        response.user?.role
                    )
                    // Asegurar rol desde /auth/me en caso de que el login no lo incluya
                    try {
                        val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthServiceAuthenticated(this@MainActivity).me() }
                        tokenManager.saveAuth(
                            token,
                            me.name,
                            me.email,
                            me.id,
                            me.role
                        )
                    } catch (_: Exception) {
                        // Si falla, continuamos con lo que tenemos
                    }
                    navigateToHome()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Login falló: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}
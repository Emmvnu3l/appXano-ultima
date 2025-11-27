package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

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
            lifecycleScope.launch {
                try {
                    val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthServiceAuthenticated(this@MainActivity).me() }
                    val st = me.status?.lowercase()?.trim()
                    if (st == "blocked") {
                        Toast.makeText(this@MainActivity, "Tu cuenta ha sido suspendida. Contacta a un administrador.", Toast.LENGTH_LONG).show()
                        tokenManager.clear()
                    } else {
                        navigateToHome()
                        return@launch
                    }
                } catch (_: Exception) {
                    navigateToHome()
                    return@launch
                }
            }
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_enter_email_password), Toast.LENGTH_SHORT).show()
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
        if (!isOnline()) {
            Toast.makeText(this, getString(R.string.msg_login_failed, "Sin conexión a Internet"), Toast.LENGTH_LONG).show()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createAuthService(this@MainActivity)
                val response = withContext(Dispatchers.IO) {
                    service.login(LoginRequest(email, password))
                }
                val token = response.effectiveToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_token_missing), Toast.LENGTH_SHORT).show()
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
                        // Llamada autenticada a /auth/me para obtener y guardar el rol del usuario.
                        val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthServiceAuthenticated(this@MainActivity).me() }
                        tokenManager.saveAuth(
                            token,
                            me.name,
                            me.email,
                            me.id,
                            me.role
                        )
                        val st = me.status?.lowercase()?.trim()
                        if (st == "blocked") {
                            Toast.makeText(this@MainActivity, "Tu cuenta ha sido suspendida. Contacta a un administrador.", Toast.LENGTH_LONG).show()
                            tokenManager.clear()
                            return@launch
                        }
                        try {
                            withContext(Dispatchers.IO) {
                                RetrofitClient.createMembersServiceAuthenticated(this@MainActivity)
                                    .updateStatus(mapOf("status" to "active"))
                            }
                        } catch (_: Exception) {}
                    } catch (_: Exception) {
                        // Si falla, continuamos con lo que tenemos
                    }
                    navigateToHome()
                }
            } catch (e: Exception) {
                val msg = if (e is UnknownHostException) "No se pudo resolver el host (verifique conexión y DNS)" else (e.message ?: "")
                Toast.makeText(this@MainActivity, getString(R.string.msg_login_failed, msg), Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToHome() {
        val tm = TokenManager(this)
        val dest = if (tm.isAdmin()) HomeActivity::class.java else LimitedHomeActivity::class.java
        startActivity(Intent(this, dest))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

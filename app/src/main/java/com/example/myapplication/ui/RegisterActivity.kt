package com.example.myapplication.ui
// Activity de registro de usuarios. Demuestra:
// - Ajuste de UI para evitar solapamientos con barras del sistema (WindowCompat.setDecorFitsSystemWindows).
// - Validaciones locales de email y password antes de llamar al backend.
// - Uso de RetrofitClient.createAuthService(this@RegisterActivity) sin token para signup.

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.model.SignupRequest
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar con flecha atrás, sin menú hamburguesa
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        

        binding.etEmail.addTextChangedListener { s ->
            val msg = validateEmail(s?.toString()?.trim().orEmpty())
            binding.tilEmail.error = msg
        }
        binding.etPassword.addTextChangedListener { s ->
            val msg = validatePassword(s?.toString().orEmpty())
            binding.tilPassword.error = msg
        }
        binding.etName.addTextChangedListener { s ->
            binding.tilName.error = if (s.isNullOrBlank()) "Nombre obligatorio" else null
        }

        binding.btnRegister.setOnClickListener { submitRegister() }
        binding.tvLogin.setOnClickListener { startActivity(android.content.Intent(this, MainActivity::class.java)) }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun submitRegister() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        var hasError = false
        if (name.isEmpty()) { binding.tilName.error = "Nombre obligatorio"; hasError = true }
        validateEmail(email)?.let { binding.tilEmail.error = it; hasError = true }
        validatePassword(password)?.let { binding.tilPassword.error = it; hasError = true }
        if (hasError) return

        val request = SignupRequest(
            name = name,
            email = email,
            password = password
        )

        setLoading(true)
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createAuthService(this@RegisterActivity)
                val response = withContext(Dispatchers.IO) { service.signup(request) }
                val token = response.effectiveToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@RegisterActivity, getString(R.string.msg_token_missing), Toast.LENGTH_SHORT).show()
                } else {
                    val tm = TokenManager(this@RegisterActivity)
                    tm.saveAuth(
                        token,
                        response.user?.name,
                        response.user?.email,
                        response.user?.id,
                        response.user?.role
                    )
                    try {
                        withContext(Dispatchers.IO) {
                            com.example.myapplication.api.RetrofitClient.createMembersServiceAuthenticated(this@RegisterActivity)
                                .updateStatus(mapOf("status" to "active"))
                        }
                    } catch (_: Exception) {}
                    Toast.makeText(this@RegisterActivity, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is java.net.UnknownHostException -> "No se pudo resolver el host (verifique conexión y DNS)"
                    else -> e.message ?: ""
                }
                Toast.makeText(this@RegisterActivity, getString(R.string.msg_register_failed, msg), Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Permite solo correos @duocuc.cl o @gmail.com y valida formato general.
     */
    private fun validateEmail(email: String): String? {
        // Normaliza a minúsculas y valida dominio permitido y patrón general de email.
        val lower = email.lowercase()
        val allowedDomain = lower.endsWith("@duocuc.cl") || lower.endsWith("@gmail.com")
        if (!allowedDomain) return "Solo se permiten correos @duocuc.cl o @gmail.com"

        // Regex email general (sencillo)
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        if (!emailRegex.matches(email)) return "Formato de correo no válido"
        return null
    }

    /**
     * Al menos 8 caracteres, con 1 dígito y 1 carácter especial.
     */
    private fun validatePassword(password: String): String? {
        if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres"
        val hasDigit = password.any { it.isDigit() }
        val specialChars = "!@#\$%^&*()-_=+{}[]:;\"'<>,.?/|\\`~"
        val hasSpecial = password.any { specialChars.contains(it) }
        if (!hasDigit) return "La contraseña debe incluir al menos un número"
        if (!hasSpecial) return "La contraseña debe incluir al menos un carácter especial"
        return null
    }
}

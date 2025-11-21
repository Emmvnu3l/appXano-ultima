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
import android.widget.ArrayAdapter
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

        val tm = TokenManager(this)
        val isAdmin = tm.isAdmin()
        if (isAdmin) {
            binding.spRole.visibility = View.VISIBLE
            val roles = listOf("user", "admin")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spRole.adapter = adapter
        } else {
            binding.spRole.visibility = View.GONE
        }

        binding.btnRegister.setOnClickListener { submitRegister() }
        // Al pulsar "Registrar", validamos y enviamos los datos al endpoint /auth/signup.
        // Ocultamos el botón "Volver" en favor de la flecha del Toolbar
        binding.btnBack.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun submitRegister() {
        // Recolecta valores del formulario; se usan .orEmpty() para evitar nulls.
        // Campo "Nombre" no visible; no lo pedimos al usuario
        val name = ""
        val firstName = binding.etFirstName.text?.toString()?.trim().orEmpty()
        val lastName = binding.etLastName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val shippingAddress = binding.etShippingAddress.text?.toString()?.trim().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_enter_email_password), Toast.LENGTH_SHORT).show()
            return
        }

        // Validaciones de formato
        // validateEmail/validatePassword devuelven un mensaje de error o null si todo está ok.
        validateEmail(email)?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            return
        }
        validatePassword(password)?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            return
        }

        val selectedRole = if (TokenManager(this).isAdmin()) {
            val pos = binding.spRole.selectedItemPosition
            if (pos >= 0) binding.spRole.selectedItem.toString() else "user"
        } else "user"

        val request = SignupRequest(
            name = name,
            email = email,
            password = password,
            first_name = firstName,
            last_name = lastName,
            role = selectedRole,
            status = "activo",
            shipping_address = shippingAddress,
            phone = phone
        )

        setLoading(true)
        lifecycleScope.launch {
            try {
                val tm = TokenManager(this@RegisterActivity)
                val isAdmin = tm.isAdmin()
                val service = if (isAdmin) {
                    RetrofitClient.createAuthServiceAuthenticated(this@RegisterActivity)
                } else {
                    RetrofitClient.createAuthService(this@RegisterActivity)
                }
                val response = withContext(Dispatchers.IO) { service.signup(request) }
                if (isAdmin) {
                    Toast.makeText(this@RegisterActivity, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val token = response.effectiveToken()
                    if (token.isNullOrEmpty()) {
                        Toast.makeText(this@RegisterActivity, getString(R.string.msg_token_missing), Toast.LENGTH_SHORT).show()
                    } else {
                        tm.saveAuth(
                            token,
                            response.user?.name,
                            response.user?.email,
                            response.user?.id,
                            response.user?.role
                        )
                        Toast.makeText(this@RegisterActivity, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, getString(R.string.msg_register_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
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
        // Reglas mínimas: longitud, al menos un dígito y un carácter especial.
        if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres"
        val hasDigit = password.any { it.isDigit() }
        val specialChars = "!@#\$%^&*()-_=+{}[]:;\"'<>,.?/|\\`~"
        val hasSpecial = password.any { specialChars.contains(it) }
        if (!hasDigit) return "La contraseña debe incluir al menos un número"
        if (!hasSpecial) return "La contraseña debe incluir al menos un carácter especial"
        return null
    }
}
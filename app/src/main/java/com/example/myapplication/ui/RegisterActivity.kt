package com.example.myapplication.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Actividad de registro con Jetpack Compose.
 * - Elementos centrados usando Column con Arrangement.Center y Alignment.CenterHorizontally
 * - Validaciones en tiempo real de email y password
 * - Spinner de carga mientras se realiza el registro (login al endpoint /auth/login)
 * - Toast al completar correctamente
 * - Botón de regreso para volver a MainActivity
 */
class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                RegisterScreen(
                    // Al pulsar registrar, llamamos al endpoint usando corutinas
                    onRegister = { email, password, setLoading ->
                        // Iniciamos loading desde la UI
                        setLoading(true)
                        // Usamos lifecycleScope del Activity para lanzar la corutina
                        lifecycleScope.launch {
                            try {
                                val service = RetrofitClient.createAuthService(this@RegisterActivity)
                                val response = withContext(Dispatchers.IO) {
                                    // Usamos signup para crear el usuario
                                    service.signup(com.example.myapplication.model.SignupRequest(email, password))
                                }
                                val token = response.effectiveToken()
                                if (token.isNullOrEmpty()) {
                                    Toast.makeText(this@RegisterActivity, "Token no recibido", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Guardamos sesión para persistencia y posible uso posterior
                                    val tm = TokenManager(this@RegisterActivity)
                                    tm.saveAuth(
                                        token,
                                        response.user?.name,
                                        response.user?.email,
                                        response.user?.id
                                    )
                                    // Mostramos toast de éxito
                                    Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    // Opcional: regresar a MainActivity
                                    finish()
                                }
                            } catch (e: Exception) {
                                val msg = when {
                                    e.message?.contains("401") == true || e.message?.contains("403") == true ->
                                        "Credenciales inválidas o usuario ya existe"
                                    else -> "Registro falló: ${e.message}"
                                }
                                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
                            } finally {
                                setLoading(false)
                            }
                        }
                    },
                    // Al pulsar volver, simplemente cerramos la actividad
                    onBack = { finish() }
                )
            }
        }
    }
}

/**
 * Composable principal de la pantalla de registro.
 * onRegister: callback que ejecuta el login al backend.
 * onBack: callback para regresar a MainActivity.
 */
@Composable
fun RegisterScreen(
    onRegister: (email: String, password: String, setLoading: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    // Estado para los campos
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Estado de loading para mostrar el spinner
    var isLoading by remember { mutableStateOf(false) }

    // Validaciones en tiempo real
    val emailError = validateEmail(email)
    val passwordError = validatePassword(password)

    // Botón habilitado sólo si no hay errores y no está cargando
    val isFormValid = emailError == null && passwordError == null && email.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Campo de email con error en tiempo real
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            placeholder = { Text("usuario@duocuc.cl / usuario@gmail.com") },
            isError = emailError != null
        )
        if (emailError != null) {
            Text(text = emailError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))

        // Campo de password con error en tiempo real
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            placeholder = { Text("Mín. 8, dígito y carácter especial") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != null
        )
        if (passwordError != null) {
            Text(text = passwordError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        // Botón de registro
        Button(
            enabled = isFormValid && !isLoading,
            onClick = { onRegister(email, password) { isLoading = it } }
        ) {
            Text("Registrar")
        }

        Spacer(Modifier.height(12.dp))

        // Spinner de carga cuando corresponde
        if (isLoading) {
            CircularProgressIndicator()
        }

        Spacer(Modifier.height(24.dp))

        // Botón para volver al login/MainActivity
        TextButton(onClick = onBack) {
            Text("Volver")
        }
    }
}

/**
 * Valida email:
 * - Formato válido general
 * - Dominio permitido: @duocuc.cl o @gmail.com
 * Retorna null si es válido o un texto de error si no.
 */
fun validateEmail(email: String): String? {
    if (email.isBlank()) return "El correo es obligatorio"
    val lower = email.lowercase()
    val domainAllowed = lower.endsWith("@duocuc.cl") || lower.endsWith("@gmail.com")
    // Patrón simple de email (podemos usar android.util.Patterns, pero mantemos independiente aquí)
    val pattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    val formatOk = pattern.matches(email)
    return when {
        !formatOk -> "Formato de correo inválido"
        !domainAllowed -> "Dominio no permitido (use @duocuc.cl o @gmail.com)"
        else -> null
    }
}

/**
 * Valida password:
 * - Al menos 8 caracteres
 * - Al menos un dígito
 * - Al menos un carácter especial
 * Retorna null si es válido o un texto de error si no.
 */
fun validatePassword(password: String): String? {
    if (password.isBlank()) return "La contraseña es obligatoria"
    // Requiere 8+ caracteres, al menos un dígito y un carácter especial
    val hasMinLength = password.length >= 8
    val hasDigit = password.any { it.isDigit() }
    val specialChars = "!@#\$%^&*()_+-=[]{};':\"\\|,.<>/?" // conjunto de caracteres especiales comunes
    val hasSpecial = password.any { specialChars.contains(it) }
    return when {
        !hasMinLength -> "Debe tener al menos 8 caracteres"
        !hasDigit -> "Debe incluir al menos un dígito"
        !hasSpecial -> "Debe incluir al menos un carácter especial"
        else -> null
    }
}
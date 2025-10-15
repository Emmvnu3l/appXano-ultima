package com.example.myapplication.ui

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import coil.load
import com.example.myapplication.api.TokenManager
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityProductDetailBinding
import com.example.myapplication.model.Product
import com.example.myapplication.ui.CreateProductActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Añadimos comportamiento básico a la barra de búsqueda:
        // - Acción de teclado "Buscar" muestra un Toast con el término.
        // - El botón de limpiar lo gestiona el TextInputLayout automáticamente.
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            // Comentario: si el usuario toca "Buscar" en el teclado,
            // navegamos a la pestaña Productos con el término para filtrar.
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString()?.trim().orEmpty()
                if (query.isNotEmpty()) {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.putExtra("open_products", true)
                    intent.putExtra("products_query", query)
                    startActivity(intent)
                } else {
                    android.widget.Toast.makeText(this, "Ingresa un término de búsqueda", android.widget.Toast.LENGTH_SHORT).show()
                }
                true
            } else false
        }
        // Modo testeo: mostrar siempre el botón
        binding.btnCreateProduct.visibility = View.VISIBLE
        val tm = TokenManager(this)
        val isAdmin = tm.isAdmin()
        binding.btnCreateProduct.setOnClickListener {
            startActivity(Intent(this, CreateProductActivity::class.java))
        }

        // Si el rol no está aún cargado, obtenerlo desde /auth/me y actualizar visibilidad
        if (tm.getRole().isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthService(this@ProductDetailActivity).me() }
                    tm.setRole(me.role)
                    // En modo test, mantenemos visible el botón
                    binding.btnCreateProduct.visibility = View.VISIBLE
                } catch (_: Exception) { /* ignorar */ }
            }
        }

        val product = intent.getSerializableExtra("product") as? Product
        if (product != null) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "$${product.price}"
            binding.tvDescription.text = product.description ?: ""
            val url = product.images?.firstOrNull()?.url
            if (url != null) binding.ivImage.load(url)
        }
    }
}
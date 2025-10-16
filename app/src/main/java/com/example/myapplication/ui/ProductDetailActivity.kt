package com.example.myapplication.ui

import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import coil.load
import com.example.myapplication.R
import com.example.myapplication.api.TokenManager
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityProductDetailBinding
import com.example.myapplication.model.Product
import com.example.myapplication.ui.NavigationHelper
import com.example.myapplication.ui.ImageViewerDialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private var quantity: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Toolbar igual que RegisterActivity: flecha que vuelve a la anterior
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Menú de edición en Toolbar
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_edit_product) {
                NavigationHelper.openAddProduct(this)
                true
            } else false
        }
        // Buscador eliminado: sin comportamiento de filtro en esta actividad
        val tm = TokenManager(this)
        val isAdmin = tm.isAdmin()

        // Si el rol no está aún cargado, obtenerlo desde /auth/me y actualizar visibilidad
        if (tm.getRole().isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthServiceAuthenticated(this@ProductDetailActivity).me() }
                    tm.setRole(me.role)
                    // nada
                } catch (_: Exception) { /* ignorar */ }
            }
        }

        val product = intent.getSerializableExtra("product") as? Product
        if (product != null) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "$${product.price}"
            binding.tvDescription.text = product.description ?: ""
            val urls = product.images?.mapNotNull { it.url }.orEmpty()
            val url = urls.firstOrNull()
            if (url != null) binding.ivImage.load(url)
            // Abrir visor al tocar la imagen
            binding.ivImage.setOnClickListener {
                if (urls.isNotEmpty()) {
                    ImageViewerDialogFragment.newInstance(urls, 0)
                        .show(supportFragmentManager, "image_viewer")
                }
            }
        }

        // Cantidad inicial
        quantity = 1
        binding.tvQuantity.text = quantity.toString()

        binding.btnMinus.setOnClickListener {
            val next = (quantity - 1).coerceAtLeast(1)
            quantity = next
            binding.tvQuantity.text = next.toString()
        }
        binding.btnPlus.setOnClickListener {
            val next = quantity + 1
            quantity = next
            binding.tvQuantity.text = next.toString()
        }

        binding.btnAddToCart.setOnClickListener {
            NavigationHelper.openAddProduct(this)
        }

        // Mostrar/ocultar más detalles
        var expanded = false
        binding.tvMore.setOnClickListener {
            expanded = !expanded
            if (expanded) {
                binding.tvDescription.maxLines = Integer.MAX_VALUE
                binding.tvMore.text = "LESS"
            } else {
                binding.tvDescription.maxLines = 3
                binding.tvMore.text = "MORE"
            }
        }

        // FAB edición (visual)
        binding.fabEdit.setOnClickListener {
            NavigationHelper.openAddProduct(this)
        }
    }
}
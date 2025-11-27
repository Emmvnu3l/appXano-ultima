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
import com.example.myapplication.api.ApiConfig
import android.net.Uri

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
        var isAdmin = tm.isAdmin()

        // Si el rol no está aún cargado, obtenerlo desde /auth/me y actualizar visibilidad
        if (tm.getRole().isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthServiceAuthenticated(this@ProductDetailActivity).me() }
                    tm.setRole(me.role)
                    isAdmin = (me.role == "admin")
                    applyRoleUi(isAdmin)
                } catch (_: Exception) { /* ignorar */ }
            }
        }

        val product = intent.getSerializableExtra("product") as? Product
        if (product != null) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "$${product.price}"
            binding.tvDescription.text = product.description ?: ""
            val urlsRaw = product.img?.mapNotNull { it.url ?: it.path }.orEmpty()
            val urls = urlsRaw.mapNotNull { sanitizeImageUrl(it) }
            val cover = urls.firstOrNull()
            if (cover != null) {
                binding.ivImage.load(cover) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    allowHardware(false)
                    memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    diskCachePolicy(coil.request.CachePolicy.ENABLED)
                }
            } else {
                binding.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
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
            val cm = CartManager(this)
            val q = quantity
            val product = intent.getSerializableExtra("product") as? Product
            if (product != null) {
                cm.add(product.id, q)
            }
        }

        // Mostrar/ocultar más detalles
        var expanded = false
        binding.tvMore.setOnClickListener {
            expanded = !expanded
            if (expanded) {
                binding.tvDescription.maxLines = Integer.MAX_VALUE
                binding.tvMore.text = getString(R.string.label_less)
            } else {
                binding.tvDescription.maxLines = 3
                binding.tvMore.text = getString(R.string.label_more)
            }
        }

        applyRoleUi(isAdmin)
    }

    private fun applyRoleUi(isAdmin: Boolean) {
        if (isAdmin) {
            binding.fabEdit.visibility = View.GONE
            binding.btnEditFixed.visibility = View.VISIBLE
            binding.btnEditFixed.setOnClickListener {
                val p = intent.getSerializableExtra("product") as? com.example.myapplication.model.Product
                if (p != null) NavigationHelper.openEditProduct(this, p)
            }
        } else {
            binding.fabEdit.visibility = View.GONE
            binding.btnEditFixed.visibility = View.GONE
        }
    }

    private fun sanitizeImageUrl(s: String?): String? {
        if (s.isNullOrBlank()) return null
        var u = s.trim()
        u = u.replace("`", "").replace("\"", "")
        u = u.replace("\n", "").replace("\r", "").replace("\t", "")
        if (u.startsWith("/")) {
            val base = ApiConfig.storeBaseUrl
            val parsed = Uri.parse(base)
            val origin = (parsed.scheme ?: "https") + "://" + (parsed.host ?: "")
            u = origin + u
        }
        if (!u.startsWith("http")) {
            u = "https://" + u.trimStart('/')
        }
        u = u.replace(" ", "%20")
        return u
    }
}

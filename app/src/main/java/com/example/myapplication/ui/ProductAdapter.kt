package com.example.myapplication.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication.R
import com.example.myapplication.model.Product
import com.example.myapplication.ui.NavigationHelper
import com.example.myapplication.api.ApiConfig
import android.net.Uri

/**
 * Adapter reestructurado con dos tipos de ítems:
 * - Header: barra de búsqueda incrustada en el RecyclerView
 * - Producto: tarjeta estilo tienda con cantidad y botón de carrito
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onResultsCountChanged: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
    }

    private var original: List<Product> = emptyList()
    private var filtered: List<Product> = emptyList()
    var initialQuery: String? = null
    var categoryIdFilter: Int? = null
    private var query: String = ""

    // Estado simple de cantidades y productos añadidos (UI-only)
    private val quantities = mutableMapOf<Int, Int>()
    private val addedToCart = mutableSetOf<Int>()

    fun setProducts(list: List<Product>) {
        original = list
        applyFilter()
        // Eliminamos notifyDataSetChanged() global para evitar crashes durante layout
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = filtered.size + 1 // + header

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_HEADER else TYPE_PRODUCT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val v = inflater.inflate(R.layout.item_products_header, parent, false)
            HeaderVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_product, parent, false)
            ProductVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderVH).bind()
        } else {
            val p = filtered[position - 1]
            (holder as ProductVH).bind(p)
        }
    }

    private fun applyFilter() {
        val q = (initialQuery ?: query).trim()
        
        filtered = original.filter { p ->
            // 1. Filtro por categoría (si se especifica)
            val catFilterMatch = categoryIdFilter?.let { it == p.category } ?: true
            
            // 2. Filtro por texto
            val textMatch = if (q.isEmpty()) true else {
                val nameMatch = p.name.contains(q, ignoreCase = true)
                val descMatch = p.description?.contains(q, ignoreCase = true) == true
                // Opcional: Búsqueda por ID de categoría como texto
                val catMatch = p.category?.toString()?.contains(q, ignoreCase = true) == true
                nameMatch || descMatch || catMatch
            }
            
            catFilterMatch && textMatch
        }
        onResultsCountChanged(filtered.size)
    }

    inner class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val et = itemView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchHeader)
        
        private var isBinding = false

        fun bind() {
            isBinding = true
            
            if (et.tag == null) {
                et.tag = "configured"
                
                et.addTextChangedListener { text ->
                    if (!isBinding) {
                        val newQuery = text?.toString().orEmpty()
                        if (query != newQuery) {
                            query = newQuery
                            initialQuery = null
                            
                            et.post {
                                applyFilter()
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
                
                et.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                        et.clearFocus()
                        true
                    } else false
                }
            }
            
            val textToShow = initialQuery ?: query
            if (et.text?.toString() != textToShow) {
                 et.setText(textToShow)
            }
            
            isBinding = false
        }
    }

    inner class ProductVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iv: ImageView = itemView.findViewById(R.id.ivImage)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val price: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnCart: Button = itemView.findViewById(R.id.btnAddToCart)

        fun bind(p: Product) {
            name.text = p.name
            price.text = "$${p.price}"
            val raw = p.images?.firstOrNull()?.let { it.url ?: it.path }
            val url = sanitizeImageUrl(raw)
            if (url != null) {
                iv.load(url) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                }
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            val qty = quantities[p.id] ?: 1
            tvQty.text = qty.toString()

            btnMinus.setOnClickListener {
                val current = (quantities[p.id] ?: 1).coerceAtLeast(1)
                val next = (current - 1).coerceAtLeast(1)
                quantities[p.id] = next
                tvQty.text = next.toString()
            }
            btnPlus.setOnClickListener {
                val current = (quantities[p.id] ?: 1)
                val next = current + 1
                quantities[p.id] = next
                tvQty.text = next.toString()
            }

            val added = addedToCart.contains(p.id)
            btnCart.text = if (added) itemView.context.getString(com.example.myapplication.R.string.label_added) else itemView.context.getString(com.example.myapplication.R.string.label_add_to_cart)
            btnCart.isEnabled = !added
            btnCart.setOnClickListener {
                val ctx = itemView.context
                val cm = CartManager(ctx)
                val q = quantities[p.id] ?: 1
                cm.add(p.id, q)
                addedToCart.add(p.id)
                btnCart.text = ctx.getString(com.example.myapplication.R.string.label_added)
                btnCart.isEnabled = false
            }

            itemView.setOnClickListener { onProductClick(p) }
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
}
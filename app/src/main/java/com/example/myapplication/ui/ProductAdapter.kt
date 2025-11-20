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
    private var query: String = ""

    // Estado simple de cantidades y productos añadidos (UI-only)
    private val quantities = mutableMapOf<Int, Int>()
    private val addedToCart = mutableSetOf<Int>()

    fun setProducts(list: List<Product>) {
        original = list
        applyFilter()
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
        filtered = if (q.isEmpty()) original else original.filter { p ->
            val nameMatch = p.name.contains(q, ignoreCase = true)
            val descMatch = p.description?.contains(q, ignoreCase = true) == true
            nameMatch || descMatch
        }
        onResultsCountChanged(filtered.size)
    }

    inner class HeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val et = itemView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearchHeader)

        fun bind() {
            // Setear query inicial en el header la primera vez
            if (!initialQuery.isNullOrEmpty()) {
                et.setText(initialQuery)
                query = initialQuery ?: ""
                initialQuery = null // consumir para evitar reseteos
            }
            et.addTextChangedListener { text ->
                query = text?.toString().orEmpty()
                applyFilter()
                notifyDataSetChanged()
            }
            // Acción de teclado "Buscar"
            et.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    query = et.text?.toString()?.trim().orEmpty()
                    applyFilter()
                    notifyDataSetChanged()
                    true
                } else false
            }
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
            val url = p.images?.firstOrNull()?.url
            if (url != null) {
                iv.load(url)
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
    }
}
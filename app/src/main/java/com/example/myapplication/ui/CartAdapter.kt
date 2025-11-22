package com.example.myapplication.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.myapplication.R
import com.example.myapplication.api.ApiConfig
import com.example.myapplication.model.Product

class CartAdapter(
    private val onQuantityChanged: (productId: Int, quantity: Int) -> Unit,
    private val onRemove: (productId: Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {
    private var products: List<Product> = emptyList()
    private var quantities: Map<Int, Int> = emptyMap()

    fun setData(products: List<Product>, quantities: Map<Int, Int>) {
        // Ordenar alfabéticamente por nombre
        this.products = products.sortedBy { it.name }
        this.quantities = quantities
        notifyDataSetChanged()
    }

    fun findProduct(id: Int): Product? = products.find { it.id == id }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = products[position]
        val qty = quantities[p.id] ?: 1
        holder.bind(p, qty)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumb: ImageView = itemView.findViewById(R.id.ivThumb)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val price: TextView = itemView.findViewById(R.id.tvPrice)
        private val qty: TextView = itemView.findViewById(R.id.tvQuantity)
        private val totalItem: TextView = itemView.findViewById(R.id.tvTotalItem)
        private val tvStockWarning: TextView = itemView.findViewById(R.id.tvStockWarning)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(p: Product, quantity: Int) {
            name.text = p.name
            price.text = "$${p.price}"
            qty.text = quantity.toString()
            
            val total = p.price * quantity
            totalItem.text = String.format("$%.2f", total)

            // Cargar imagen
            val raw = p.images?.firstOrNull()?.let { it.url ?: it.path }
            val url = sanitizeImageUrl(raw)
            if (url != null) {
                ivThumb.load(url) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    transformations(RoundedCornersTransformation(8f))
                }
            } else {
                ivThumb.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Stock check
            val maxStock = p.stock ?: 999
            if (quantity >= maxStock) {
                tvStockWarning.visibility = View.VISIBLE
                tvStockWarning.text = "Máx: $maxStock"
                btnPlus.isEnabled = false
                btnPlus.alpha = 0.5f
            } else {
                tvStockWarning.visibility = View.GONE
                btnPlus.isEnabled = true
                btnPlus.alpha = 1.0f
            }

            btnMinus.setOnClickListener {
                val current = qty.text.toString().toIntOrNull() ?: 1
                val next = (current - 1).coerceAtLeast(0)
                onQuantityChanged(p.id, next)
            }
            btnPlus.setOnClickListener {
                val current = qty.text.toString().toIntOrNull() ?: 1
                if (current < maxStock) {
                    val next = current + 1
                    onQuantityChanged(p.id, next)
                }
            }
            btnRemove.setOnClickListener {
                onRemove(p.id)
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
}
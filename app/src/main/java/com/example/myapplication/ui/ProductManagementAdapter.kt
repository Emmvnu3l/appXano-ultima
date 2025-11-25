package com.example.myapplication.ui

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
import android.net.Uri

class ProductManagementAdapter(
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductManagementAdapter.VH>() {

    private val items = mutableListOf<Product>()

    fun submitList(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_management, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(product: Product) {
            tvName.text = product.name
            tvPrice.text = "$${product.price}"
            tvStock.text = "Stock: ${product.stock ?: 0}"
            
            val raw = product.img?.firstOrNull()?.let { it.url ?: it.path }
            val url = sanitizeImageUrl(raw)
            
            if (url != null) {
                ivProduct.load(url) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                    transformations(RoundedCornersTransformation(8f))
                    allowHardware(false)
                    memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    diskCachePolicy(coil.request.CachePolicy.ENABLED)
                }
            } else {
                ivProduct.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            btnEdit.setOnClickListener { onEditClick(product) }
            btnDelete.setOnClickListener { onDeleteClick(product) }
        }
        
        private fun sanitizeImageUrl(s: String?): String? {
            if (s.isNullOrBlank()) return null
            var u = s.trim()
            u = u.replace("`", "").replace("\"", "")
            u = u.replace("\n", "").replace("\r", "").replace("\t", "")
            if (!u.startsWith("http")) {
                val base = ApiConfig.storeBaseUrl
                u = base.trimEnd('/') + "/" + u.trimStart('/')
            }
            u = u.replace(" ", "%20")
            return u
        }
    }
}

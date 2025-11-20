package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Product

class CartAdapter(
    private val onQuantityChanged: (productId: Int, quantity: Int) -> Unit,
    private val onRemove: (productId: Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {
    private var products: List<Product> = emptyList()
    private var quantities: Map<Int, Int> = emptyMap()

    fun setData(products: List<Product>, quantities: Map<Int, Int>) {
        this.products = products
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
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val price: TextView = itemView.findViewById(R.id.tvPrice)
        private val qty: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(p: Product, quantity: Int) {
            name.text = p.name
            price.text = "$${p.price}"
            qty.text = quantity.toString()

            btnMinus.setOnClickListener {
                val current = qty.text.toString().toIntOrNull() ?: 1
                val next = (current - 1).coerceAtLeast(0)
                onQuantityChanged(p.id, next)
            }
            btnPlus.setOnClickListener {
                val current = qty.text.toString().toIntOrNull() ?: 1
                val next = current + 1
                onQuantityChanged(p.id, next)
            }
            btnRemove.setOnClickListener {
                onRemove(p.id)
            }
        }
    }
}
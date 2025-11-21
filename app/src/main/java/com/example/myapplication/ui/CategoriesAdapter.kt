package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Category
import coil.load
import coil.transform.RoundedCornersTransformation

class CategoriesAdapter(
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.VH>() {

    private val items = mutableListOf<Category>()

    fun submitList(newItems: List<Category>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCategory: ImageView = itemView.findViewById(R.id.ivCategory)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)

        fun bind(category: Category) {
            tvName.text = category.name
            
            // Carga de imagen usando Coil
            val imageUrl = category.image?.url
            if (!imageUrl.isNullOrEmpty()) {
                ivCategory.load(imageUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                    transformations(RoundedCornersTransformation(
                        topLeft = 12f, 
                        topRight = 12f
                    ))
                }
            } else {
                ivCategory.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }
}
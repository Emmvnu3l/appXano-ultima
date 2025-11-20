package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.User

class UsersAdapter(
    private val onToggleBlocked: (User, Boolean) -> Unit,
    private val onEdit: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {
    private val items: MutableList<User> = mutableListOf()

    fun setData(list: List<User>, append: Boolean) {
        if (!append) items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.imgAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val email: TextView = itemView.findViewById(R.id.tvEmail)
        private val created: TextView = itemView.findViewById(R.id.tvCreated)
        private val swBlocked: Switch = itemView.findViewById(R.id.swBlocked)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)

        fun bind(u: User) {
            // avatar: placeholder
            name.text = u.name
            email.text = u.email
            created.text = u.createdAt ?: ""
            swBlocked.isChecked = u.blocked
            swBlocked.setOnCheckedChangeListener { _, checked -> onToggleBlocked(u, checked) }
            btnEdit.setOnClickListener { onEdit(u) }
        }
    }
}
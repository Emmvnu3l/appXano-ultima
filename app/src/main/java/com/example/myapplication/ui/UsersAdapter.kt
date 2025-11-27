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
    private val onChangeStatus: (User, String) -> Unit,
    private val onEdit: (User) -> Unit,
    private val onView: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {
    private val items: MutableList<User> = mutableListOf()

    companion object {
        fun computeStatus(u: com.example.myapplication.model.User): Pair<Boolean, String> {
            val raw = (u.status ?: "").lowercase().trim()
            val isBlocked = raw == "blocked" || u.blocked
            val label = if (isBlocked) "bloqueado" else when (raw) {
                "disconnected" -> "desconectado"
                "inactive" -> "inactivo"
                "active" -> "activo"
                "unlocked" -> "activo"
                else -> raw.ifEmpty { "activo" }
            }
            android.util.Log.d("UsersAdapter", "bind user=${u.id} status=${u.status} blocked=${u.blocked} -> isBlocked=$isBlocked label=$label")
            return isBlocked to label
        }
    }

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
        private val btnToggleStatus: Button = itemView.findViewById(R.id.btnToggleStatus)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private var suppress = false

        fun bind(u: User) {
            name.text = u.name
            email.text = u.email
            val createdText = u.createdAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(it)) } ?: ""
            created.text = createdText
            val (isBlocked, label) = UsersAdapter.computeStatus(u)
            tvStatus.text = label
            btnToggleStatus.text = if (isBlocked) "Desbloquear" else "Bloquear"
            btnToggleStatus.setOnClickListener {
                val next = if (isBlocked) "active" else "blocked"
                onChangeStatus(u, next)
            }
            btnEdit.setOnClickListener { onEdit(u) }
            itemView.setOnClickListener { onView(u) }
        }
    }

    fun setBlocked(userId: Int, blocked: Boolean) {
        val idx = items.indexOfFirst { it.id == userId }
        if (idx >= 0) {
            val newStatus = if (blocked) "blocked" else "active"
            items[idx] = items[idx].copy(blocked = blocked, status = newStatus)
            notifyItemChanged(idx)
        }
    }
}

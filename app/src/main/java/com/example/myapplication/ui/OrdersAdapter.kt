package com.example.myapplication.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.model.Order

class OrdersAdapter(
    private val onChangeState: (Order, String) -> Unit,
    private val onCancelWithReason: (Order) -> Unit,
    private val onViewDetails: (Order) -> Unit,
    private val onSelectionChanged: (Set<Int>) -> Unit,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<OrdersAdapter.VH>() {
    private val items: MutableList<Order> = mutableListOf()
    private val selected: MutableSet<Int> = mutableSetOf()

    fun setData(list: List<Order>, append: Boolean) {
        if (!append) items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tvId)
        private val tvClient: TextView = itemView.findViewById(R.id.tvClient)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnAccept: ImageButton = itemView.findViewById(R.id.btnAccept)
        private val btnReject: ImageButton = itemView.findViewById(R.id.btnReject)
        private val btnShip: ImageButton = itemView.findViewById(R.id.btnShip)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
        private val cbSelect: android.widget.CheckBox = itemView.findViewById(R.id.cbSelect)

        fun bind(o: Order) {
            val nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es","CL"))
            val created = o.createdAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "-"
            tvId.text = "#${o.id} ${created}\t${nf.format(o.total)}"
            tvId.setTextColor(Color.WHITE)
            tvClient.visibility = View.GONE
            tvTotal.visibility = View.GONE
            tvStatus.text = o.status
            tvStatus.setTextColor(android.graphics.Color.WHITE)
            tvDate.visibility = View.GONE

            val tint = when (o.status.lowercase()) {
                "pendiente" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#616161"))
                "en_proceso" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
                "completada" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))
                "cancelada" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C62828"))
                "confirmada" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))
                "enviado" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1565C0"))
                "aceptado" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2E7D32"))
                "rechazado" -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C62828"))
                else -> android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#616161"))
            }
            androidx.core.view.ViewCompat.setBackgroundTintList(tvStatus, tint)

            itemView.setOnClickListener { onViewDetails(o) }

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = selected.contains(o.id)
            cbSelect.setOnCheckedChangeListener { _, checked ->
                if (checked) selected.add(o.id) else selected.remove(o.id)
                onSelectionChanged(selected)
            }

            if (!isAdmin) {
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnShip.visibility = View.GONE
                btnMore.visibility = View.GONE
                return
            }

            val st = o.status.lowercase()
            val acts = actionsForStatus(st)
            val showProcess = acts.first
            val showComplete = acts.second
            val showCancel = acts.third

            btnAccept.visibility = if (showProcess) View.VISIBLE else View.GONE
            btnReject.visibility = if (showCancel) View.VISIBLE else View.GONE
            btnShip.visibility = if (showComplete) View.VISIBLE else View.GONE

            btnAccept.setOnClickListener { onChangeState(o, "en_proceso") }
            btnReject.setOnClickListener { onCancelWithReason(o) }
            btnShip.setOnClickListener { onChangeState(o, "completada") }

            btnMore.visibility = View.VISIBLE
            btnMore.setOnClickListener { v ->
                val menu = android.widget.PopupMenu(v.context, v)
                val options = mutableListOf<String>()
                if (showProcess) options.add("en_proceso")
                if (showComplete) options.add("completada")
                if (showCancel) options.add("cancelada")
                options.forEachIndexed { idx, opt -> menu.menu.add(0, idx, idx, opt) }
                menu.setOnMenuItemClickListener { mi ->
                    val opt = options[mi.itemId]
                    if (opt == "cancelada") onCancelWithReason(o) else onChangeState(o, opt)
                    true
                }
                menu.show()
            }
        }

        private fun statusColor(status: String): Int = Color.DKGRAY
    }

    companion object {
        fun actionsForStatus(status: String): Triple<Boolean, Boolean, Boolean> {
            val st = status.lowercase()
            val showProcess = st == "pendiente"
            val showComplete = st == "en_proceso"
            val showCancel = st == "pendiente" || st == "en_proceso"
            return Triple(showProcess, showComplete, showCancel)
        }
    }

    fun getSelectedIds(): Set<Int> = selected
}

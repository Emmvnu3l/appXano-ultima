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
    private val onProcess: (Order) -> Unit,
    private val onComplete: (Order) -> Unit,
    private val onCancel: (Order) -> Unit,
    private val onViewDetails: (Order) -> Unit,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<OrdersAdapter.VH>() {
    private val items: MutableList<Order> = mutableListOf()

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
        private val btnAccept: ImageButton = itemView.findViewById(R.id.btnAccept)
        private val btnReject: ImageButton = itemView.findViewById(R.id.btnReject)
        private val btnShip: ImageButton = itemView.findViewById(R.id.btnShip)

        fun bind(o: Order) {
            tvId.text = "#${o.id}"
            tvClient.text = "Cliente: ${o.userId ?: "-"}"
            tvTotal.text = "$${o.total}"
            tvStatus.text = o.status
            tvStatus.setTextColor(statusColor(o.status))

            itemView.setOnClickListener { onViewDetails(o) }

            if (!isAdmin) {
                btnAccept.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnShip.visibility = View.GONE
                return
            }

            val st = o.status.lowercase()
            val showProcess = st == "pendiente"
            val showComplete = st == "en_proceso"
            val showCancel = st == "pendiente" || st == "en_proceso"

            btnAccept.visibility = if (showProcess) View.VISIBLE else View.GONE
            btnReject.visibility = if (showCancel) View.VISIBLE else View.GONE
            btnShip.visibility = if (showComplete) View.VISIBLE else View.GONE

            btnAccept.setOnClickListener { onProcess(o) }
            btnReject.setOnClickListener { onCancel(o) }
            btnShip.setOnClickListener { onComplete(o) }
        }

        private fun statusColor(status: String): Int = when (status.lowercase()) {
            "pendiente" -> Color.parseColor("#FFA000")
            "en_proceso" -> Color.parseColor("#1976D2")
            "completada" -> Color.parseColor("#2E7D32")
            "cancelada" -> Color.parseColor("#C62828")
            "confirmada" -> Color.parseColor("#2E7D32")
            "aceptado" -> Color.parseColor("#2E7D32")
            "rechazado" -> Color.parseColor("#C62828")
            "enviado" -> Color.parseColor("#1565C0")
            else -> Color.DKGRAY
        }
    }
}
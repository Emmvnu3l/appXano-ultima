package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.ui.StateUi
import com.example.myapplication.databinding.FragmentOrdersBinding
import com.example.myapplication.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrdersFragment : Fragment() {
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: OrdersAdapter
    private var page = 1
    private var loading = false
    private var endReached = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tm = com.example.myapplication.api.TokenManager(requireContext())
        adapter = OrdersAdapter(
            onProcess = { confirmAndCall(it, "pasar a en proceso") { toProcess(it) } },
            onComplete = { confirmAndCall(it, "marcar completada") { toComplete(it) } },
            onCancel = { promptCancel(it) },
            onViewDetails = { showDetails(it) },
            isAdmin = tm.isAdmin()
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }

        setupFilters()

        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dy > 0) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    val visible = lm.childCount
                    val total = lm.itemCount
                    val first = lm.findFirstVisibleItemPosition()
                    if (!loading && !endReached && visible + first >= total - 2) {
                        loadMore()
                    }
                }
            }
        })

        refresh()
    }

    private fun setLoading(loading: Boolean) {
        this.loading = loading
        if (loading) StateUi.showLoading(binding.state) else StateUi.hide(binding.state)
        binding.swipeRefresh.isRefreshing = false
    }

    private fun refresh() {
        page = 1
        endReached = false
        fetch(page, append = false)
    }

    private fun loadMore() {
        if (loading) return
        fetch(page + 1, append = true)
    }

    private fun fetch(targetPage: Int, append: Boolean) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createOrderService(requireContext())
                val list = withContext(Dispatchers.IO) { service.listOrders(currentStatusFilter, targetPage, 10) }
                val sorted = sortOrders(list)
                if (sorted.isEmpty()) endReached = true else page = targetPage
                adapter.setData(sorted, append)
                binding.state.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.state.tvError.visibility = View.VISIBLE
                binding.state.tvError.text = com.example.myapplication.api.NetworkError.message(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun confirmAndCall(o: Order, actionName: String, fn: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar")
            .setMessage("¿Deseas ${actionName} la orden #${o.id}?")
            .setPositiveButton("Sí") { _, _ -> fn() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun toProcess(o: Order) = changeState { RetrofitClient.createOrderService(requireContext()).updateStatus(o.id, com.example.myapplication.model.UpdateOrderStatusRequest("en_proceso")) }
    private fun toComplete(o: Order) = changeState { RetrofitClient.createOrderService(requireContext()).updateStatus(o.id, com.example.myapplication.model.UpdateOrderStatusRequest("completada")) }

    private fun changeState(block: suspend () -> Order) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
                refresh()
            } catch (e: Exception) {
                binding.state.tvError.visibility = View.VISIBLE
                binding.state.tvError.text = com.example.myapplication.api.NetworkError.message(e)
            } finally {
                setLoading(false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var currentStatusFilter: String? = null
    private var currentSort: String = "fecha_desc"

    private fun setupFilters() {
        val statuses = listOf("todos", "pendiente", "en_proceso", "completada", "cancelada")
        val sortOptions = listOf("fecha_desc", "fecha_asc", "estado_asc", "estado_desc")

        val spStatus = binding.root.findViewById<android.widget.Spinner>(com.example.myapplication.R.id.spStatus)
        val spSort = binding.root.findViewById<android.widget.Spinner>(com.example.myapplication.R.id.spSort)
        spStatus.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses)
        spSort.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spStatus.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                currentStatusFilter = statuses[position].takeIf { it != "todos" }
                refresh()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        spSort.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSort = sortOptions[position]
                refresh()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun sortOrders(list: List<Order>): List<Order> = when (currentSort) {
        "fecha_asc" -> list.sortedBy { it.createdAt ?: 0L }
        "estado_asc" -> list.sortedBy { it.status }
        "estado_desc" -> list.sortedByDescending { it.status }
        else -> list.sortedByDescending { it.createdAt ?: 0L }
    }

    private fun showDetails(o: Order) {
        val sb = StringBuilder()
        sb.appendLine("Orden #${o.id}")
        sb.appendLine("Estado: ${o.status}")
        sb.appendLine("Total: $${o.total}")
        val created = o.createdAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "-"
        sb.appendLine("Creada: $created")
        sb.appendLine("Items:")
        o.items.forEach { sb.appendLine("• Producto ${it.productId} x${it.quantity} (${it.price ?: 0.0})") }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Detalles de la orden")
            .setMessage(sb.toString())
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun promptCancel(o: Order) {
        val input = android.widget.EditText(requireContext())
        input.hint = "Motivo de cancelación"
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancelar orden #${o.id}")
            .setView(input)
            .setPositiveButton("Cancelar") { _, _ ->
                val reason = input.text?.toString()?.trim()
                if (reason.isNullOrEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "Motivo obligatorio", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    changeState { RetrofitClient.createOrderService(requireContext()).updateStatus(o.id, com.example.myapplication.model.UpdateOrderStatusRequest("cancelada", reason = reason, adminId = com.example.myapplication.api.TokenManager(requireContext()).getUserId())) }
                }
            }
            .setNegativeButton("Volver", null)
            .show()
    }
}
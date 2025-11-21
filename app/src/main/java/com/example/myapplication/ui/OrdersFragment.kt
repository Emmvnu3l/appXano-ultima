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
    private var pageSize = 20
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
            onChangeState = { o, target -> confirmAndCall(o, "cambiar a $target") { changeTo(o, target) } },
            onCancelWithReason = { promptCancel(it) },
            onViewDetails = { showDetails(it) },
            onSelectionChanged = { updateBulkUi(it) },
            isAdmin = tm.isAdmin()
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }

        setupFilters()
        setupPager()

        arguments?.getInt("limit")?.let { if (it > 0) pageSize = it }

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
                val tm = com.example.myapplication.api.TokenManager(requireContext())
                val ownOnly = !tm.isAdmin()
                val userIdParam = if (ownOnly) tm.getUserId() else null
                val list = withContext(Dispatchers.IO) { service.listOrders(currentStatusFilter, userIdParam, userIdParam, null, null, dateFrom, dateTo, currentTypeFilter) }
                val mine = if (ownOnly) filterByUserOrDiscount(list, tm.getUserId()) else list
                val onlyValid = ownOnly && currentStatusFilter == null
                val filtered = applyClientFilters(mine, currentStatusFilter, dateFrom, dateTo, currentTypeFilter, amountMin, amountMax, onlyValid)
                totalCount = filtered.size
                val sorted = sortOrders(filtered, currentSort)
                val pageData = slicePage(sorted, targetPage, pageSize)
                endReached = pageData.isEmpty() || (targetPage * pageSize) >= totalCount
                if (pageData.isEmpty()) {
                    binding.state.tvEmpty.text = "Sin resultados con filtros"
                } else page = targetPage
                adapter.setData(pageData, append = false)
                binding.state.tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                updatePagerUi()
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
    private fun changeTo(o: Order, target: String) = changeState { RetrofitClient.createOrderService(requireContext()).updateStatus(o.id, com.example.myapplication.model.UpdateOrderStatusRequest(target)) }

    private fun changeState(block: suspend () -> Order) {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updated = withContext(Dispatchers.IO) { block() }
                appendHistory(updated.id, updated.status, null)
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
    private var currentTypeFilter: String? = null
    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var currentSort: String = "fecha_desc"
    private var amountMin: Double? = null
    private var amountMax: Double? = null
    private var totalCount: Int = 0

    private fun setupFilters() {
        val statuses = listOf("todos", "pendiente", "confirmada", "enviado", "aceptado", "rechazado", "en_proceso", "completada", "cancelada")
        val sortOptions = listOf("fecha_desc", "fecha_asc", "estado_asc", "estado_desc")
        val types = listOf("todos", "online", "tienda")
        val pageSizes = listOf("20", "30", "50")

        val spStatus = binding.root.findViewById<android.widget.Spinner>(com.example.myapplication.R.id.spStatus)
        val spSort = binding.root.findViewById<android.widget.Spinner>(com.example.myapplication.R.id.spSort)
        val spType = binding.root.findViewById<android.widget.Spinner>(com.example.myapplication.R.id.spType)
        val spPageSize = binding.root.findViewById<android.widget.Spinner>(com.example.myapplication.R.id.spPageSize)
        val etFrom = binding.root.findViewById<android.widget.EditText>(com.example.myapplication.R.id.etFrom)
        val etTo = binding.root.findViewById<android.widget.EditText>(com.example.myapplication.R.id.etTo)
        val etAmountMin = binding.root.findViewById<android.widget.EditText>(com.example.myapplication.R.id.etAmountMin)
        val etAmountMax = binding.root.findViewById<android.widget.EditText>(com.example.myapplication.R.id.etAmountMax)
        val btnApply = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnApply)
        spStatus.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses)
        spSort.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
        spType.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spPageSize.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, pageSizes)
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
        spType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                currentTypeFilter = types[position].takeIf { it != "todos" }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        spPageSize.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                pageSize = pageSizes[position].toInt()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        btnApply.setOnClickListener {
            dateFrom = parseDate(etFrom.text?.toString())
            dateTo = parseDate(etTo.text?.toString())
            amountMin = etAmountMin.text?.toString()?.toDoubleOrNull()
            amountMax = etAmountMax.text?.toString()?.toDoubleOrNull()
            refresh()
        }

        btnApply.setOnLongClickListener {
            val target = currentStatusFilter ?: "en_proceso"
            applyBulk(target)
            true
        }
    }

    companion object {
        fun sortOrders(list: List<Order>, sort: String): List<Order> = when (sort) {
            "fecha_asc" -> list.sortedBy { it.createdAt ?: 0L }
            "estado_asc" -> list.sortedBy { it.status }
            "estado_desc" -> list.sortedByDescending { it.status }
            else -> list.sortedByDescending { it.createdAt ?: 0L }
        }
        fun applyClientFilters(list: List<Order>, status: String?, from: Long?, to: Long?, type: String?, amountMin: Double?, amountMax: Double?, onlyValid: Boolean): List<Order> {
            return list.filter { o ->
                val stOk = status?.let { o.status.equals(it, ignoreCase = true) } ?: true
                val fromOk = from?.let { (o.createdAt ?: 0L) >= it } ?: true
                val toOk = to?.let { (o.createdAt ?: 0L) <= it } ?: true
                val amountMinOk = amountMin?.let { o.total >= it } ?: true
                val amountMaxOk = amountMax?.let { o.total <= it } ?: true
                val validStatusOk = if (onlyValid) {
                    val valid = setOf("pendiente","confirmada","en_proceso","enviado","aceptado","completada")
                    valid.contains(o.status.lowercase())
                } else true
                stOk && fromOk && toOk && amountMinOk && amountMaxOk && validStatusOk
            }
        }
        fun filterByUserOrDiscount(list: List<Order>, userId: Int?): List<Order> {
            if (userId == null) return emptyList()
            return list.filter { it.userId == userId || it.discountCodeId == userId }
        }
        fun parseDate(s: String?): Long? {
            if (s.isNullOrBlank()) return null
            return try {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                fmt.parse(s)?.time
            } catch (_: Exception) { null }
        }
        fun slicePage(list: List<Order>, page: Int, pageSize: Int): List<Order> {
            val start = (page - 1) * pageSize
            if (start >= list.size || page <= 0) return emptyList()
            val end = kotlin.math.min(start + pageSize, list.size)
            return list.subList(start, end)
        }
    }

    private fun setupPager() {
        val btnPrev = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnPrev)
        val btnNext = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnNext)
        val tvPage = binding.root.findViewById<android.widget.TextView>(com.example.myapplication.R.id.tvPage)
        val etPage = binding.root.findViewById<android.widget.EditText>(com.example.myapplication.R.id.etPage)
        val btnGo = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnGoPage)
        fun update() {
            val maxPage = kotlin.math.max(1, (totalCount + pageSize - 1) / pageSize)
            tvPage.text = "Página $page de $maxPage"
            btnPrev.isEnabled = page > 1
            btnNext.isEnabled = page < maxPage
        }
        btnPrev.setOnClickListener {
            if (page > 1) fetch(page - 1, append = false)
        }
        btnNext.setOnClickListener {
            fetch(page + 1, append = false)
        }
        btnGo.setOnClickListener {
            val num = etPage.text?.toString()?.toIntOrNull()
            val maxPage = kotlin.math.max(1, (totalCount + pageSize - 1) / pageSize)
            if (num != null && num in 1..maxPage) fetch(num, append = false)
        }
        update()
    }

    private fun showDetails(o: Order) {
        val sb = StringBuilder()
        sb.appendLine("Orden #${o.id}")
        sb.appendLine("Estado: ${o.status}")
        sb.appendLine("Total: $${o.total}")
        val created = o.createdAt?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "-"
        sb.appendLine("Creada: $created")
        sb.appendLine("Items:")
        o.items.orEmpty().forEach { sb.appendLine("• Producto ${it.productId} x${it.quantity} (${it.price ?: 0.0})") }
        val hist = loadHistory(o.id)
        if (hist.isNotEmpty()) {
            sb.appendLine("\nHistorial de estado:")
            hist.forEach { sb.appendLine("• $it") }
        }
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
                    changeState {
                        val res = RetrofitClient.createOrderService(requireContext()).updateStatus(o.id, com.example.myapplication.model.UpdateOrderStatusRequest("cancelada", reason = reason, adminId = com.example.myapplication.api.TokenManager(requireContext()).getUserId()))
                        appendHistory(res.id, "cancelada", reason)
                        res
                    }
                    android.util.Log.i("Audit", "cancelada by ${com.example.myapplication.api.TokenManager(requireContext()).getUserId()} reason=$reason order=${o.id}")
                }
            }
            .setNegativeButton("Volver", null)
            .show()
    }

    private fun updateBulkUi(selected: Set<Int>) {
        val btnApplyBulk = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnApply)
        btnApplyBulk.isEnabled = selected.isNotEmpty()
    }

    private fun applyBulk(target: String) {
        val ids = adapter.getSelectedIds()
        if (ids.isEmpty()) return
        val tm = com.example.myapplication.api.TokenManager(requireContext())
        if (!tm.isAdmin()) {
            android.widget.Toast.makeText(requireContext(), "Solo administradores", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        confirmAndCallSimple("Aplicar '$target' a ${ids.size} órdenes") {
            setLoading(true)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.createOrderService(requireContext())
                    withContext(Dispatchers.IO) {
                        ids.forEach { id -> service.updateStatus(id, com.example.myapplication.model.UpdateOrderStatusRequest(target, adminId = tm.getUserId())) }
                    }
                    android.util.Log.i("Audit", "bulk $target by ${tm.getUserId()} ids=$ids")
                    refresh()
                } catch (e: Exception) {
                    StateUi.showError(binding.state, com.example.myapplication.api.NetworkError.message(e))
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private fun confirmAndCallSimple(msg: String, fn: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar")
            .setMessage(msg)
            .setPositiveButton("Sí") { _, _ -> fn() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updatePagerUi() {
        val tvPage = binding.root.findViewById<android.widget.TextView>(com.example.myapplication.R.id.tvPage)
        val btnPrev = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnPrev)
        val btnNext = binding.root.findViewById<android.widget.Button>(com.example.myapplication.R.id.btnNext)
        val maxPage = kotlin.math.max(1, (totalCount + pageSize - 1) / pageSize)
        tvPage.text = "Página $page de $maxPage"
        btnPrev.isEnabled = page > 1
        btnNext.isEnabled = page < maxPage
    }

    private fun appendHistory(orderId: Int, status: String, comment: String?) {
        try {
            val prefs = requireContext().getSharedPreferences("orders_history", android.content.Context.MODE_PRIVATE)
            val ts = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            val line = "$ts - $status" + (comment?.let { " - $it" } ?: "")
            val key = "order_$orderId"
            val cur = prefs.getString(key, "") ?: ""
            val newVal = if (cur.isBlank()) line else (cur + "\n" + line)
            prefs.edit().putString(key, newVal).apply()
        } catch (_: Exception) {}
    }

    private fun loadHistory(orderId: Int): List<String> {
        return try {
            val prefs = requireContext().getSharedPreferences("orders_history", android.content.Context.MODE_PRIVATE)
            val s = prefs.getString("order_$orderId", "") ?: ""
            s.lines().filter { it.isNotBlank() }
        } catch (_: Exception) { emptyList() }
    }
}
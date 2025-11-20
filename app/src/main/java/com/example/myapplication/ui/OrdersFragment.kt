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
        adapter = OrdersAdapter(
            onAccept = { confirmAndCall(it, "aceptar") { accept(it) } },
            onReject = { confirmAndCall(it, "rechazar") { reject(it) } },
            onShip = { confirmAndCall(it, "marcar enviado") { ship(it) } }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }

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
                val list = withContext(Dispatchers.IO) { service.listOrders("pendiente", targetPage, 10) }
                if (list.isEmpty()) endReached = true else page = targetPage
                adapter.setData(list, append)
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

    private fun accept(o: Order) = changeState { RetrofitClient.createOrderService(requireContext()).accept(o.id) }
    private fun reject(o: Order) = changeState { RetrofitClient.createOrderService(requireContext()).reject(o.id) }
    private fun ship(o: Order) = changeState { RetrofitClient.createOrderService(requireContext()).ship(o.id) }

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
}
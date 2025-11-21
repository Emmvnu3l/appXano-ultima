package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentCartBinding
import com.example.myapplication.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartFragment : Fragment() {
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        adapter = CartAdapter(
            onQuantityChanged = { productId, quantity ->
                CartManager(requireContext()).update(productId, quantity)
                updateTotal()
            },
            onRemove = { productId ->
                CartManager(requireContext()).remove(productId)
                loadCart()
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.btnCheckout.setOnClickListener {
            NavigationHelper.openCheckout(requireContext())
        }

        loadCart()
    }

    private fun loadCart() {
        val cm = CartManager(requireContext())
        val items = cm.getItems()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { service.getProducts() }
                val list = products.filter { items.keys.contains(it.id) }
                adapter.setData(list, items)
                updateTotal()
                binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) {
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun updateTotal() {
        val cm = CartManager(requireContext())
        val items = cm.getItems()
        var total = 0.0
        for ((id, qty) in items) {
            val p = adapter.findProduct(id)
            if (p != null) total += (p.price * qty)
        }
        binding.tvTotal.text = getString(R.string.cart_total_format, total)
        binding.btnCheckout.isEnabled = items.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
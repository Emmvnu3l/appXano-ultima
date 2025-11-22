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
                val cm = CartManager(requireContext())
                if (quantity <= 0) {
                    cm.remove(productId)
                    showNotification("Producto eliminado")
                } else {
                    cm.update(productId, quantity)
                    // No reload necessary if we just update locally, but adapter needs reload to sort or validate
                }
                loadCart(refreshProducts = false) // reloadCart to refresh UI totals and adapter list if sorting changed or item removed
            },
            onRemove = { productId ->
                CartManager(requireContext()).remove(productId)
                showNotification("Producto eliminado")
                loadCart(refreshProducts = false)
            }
        )
        
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        // Animaciones por defecto
        binding.recycler.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        binding.btnCheckout.setOnClickListener {
            NavigationHelper.openCheckout(requireContext())
        }

        loadCart(refreshProducts = true)
    }

    // refreshProducts: if true, fetches details from API. If false, uses cached/current logic if possible, 
    // but since we don't cache products list in Fragment property (except inside adapter), we might need to fetch or store it.
    // Let's store products in a variable.
    private var cachedProducts: List<Product> = emptyList()

    private fun loadCart(refreshProducts: Boolean) {
        val cm = CartManager(requireContext())
        val items = cm.getItems()
        
        if (items.isEmpty()) {
             adapter.setData(emptyList(), emptyMap())
             updateTotal(emptyList(), items)
             binding.tvEmpty.visibility = View.VISIBLE
             binding.recycler.visibility = View.GONE
             return
        } else {
             binding.tvEmpty.visibility = View.GONE
             binding.recycler.visibility = View.VISIBLE
        }

        if (refreshProducts || cachedProducts.isEmpty()) {
             viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.createProductService(requireContext())
                    val products = withContext(Dispatchers.IO) { service.getProducts() }
                    cachedProducts = products
                    updateUI(items)
                } catch (_: Exception) {
                    // If error, try to use cached if available or show empty/error
                    if (cachedProducts.isNotEmpty()) updateUI(items)
                }
            }
        } else {
            updateUI(items)
        }
    }

    private fun updateUI(items: Map<Int, Int>) {
        val list = cachedProducts.filter { items.keys.contains(it.id) }
        adapter.setData(list, items)
        updateTotal(list, items)
    }

    private fun updateTotal(products: List<Product>, items: Map<Int, Int>) {
        var total = 0.0
        val map = products.associateBy { it.id }
        for ((id, qty) in items) {
            val p = map[id]
            if (p != null) total += (p.price * qty)
        }
        binding.tvTotal.text = getString(R.string.cart_total_format, total)
        binding.btnCheckout.isEnabled = items.isNotEmpty()
    }
    
    private fun showNotification(msg: String) {
        com.google.android.material.snackbar.Snackbar.make(binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
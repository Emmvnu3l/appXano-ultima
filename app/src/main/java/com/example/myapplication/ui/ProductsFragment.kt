package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.NetworkError
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.FragmentProductsBinding
import com.example.myapplication.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import com.example.myapplication.ui.StateUi
import android.util.Log
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.UnknownHostException

class ProductsFragment : Fragment() {
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductAdapter
    private var initialQuery: String? = null
    private var initialCategoryId: Int? = null
    private var cartPrefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    companion object {
        fun newInstance(query: String?, categoryId: Int? = null): ProductsFragment {
            val f = ProductsFragment()
            val args = Bundle()
            args.putString("query", query)
            if (categoryId != null) args.putInt("category_id", categoryId)
            f.arguments = args
            return f
        }

        fun normalize(s: String?): String {
            if (s.isNullOrBlank()) return ""
            val n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            return n.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
                .lowercase()
                .replace("'", "")
                .replace("\"", "")
                .replace("`", "")
                .trim()
        }

        fun filterProducts(
            original: List<com.example.myapplication.model.Product>,
            query: String,
            categoryIdFilter: Int?,
            categoryNames: Map<Int, String>
        ): List<com.example.myapplication.model.Product> {
            val normQ = normalize(query)
            val tokens = normQ.split(" ").filter { it.isNotBlank() }

            val exactCategoryId = categoryNames.entries.firstOrNull { normalize(it.value) == normQ }?.key

            return original.filter { p ->
                val catFilterMatch = categoryIdFilter?.let { it == p.category } ?: true
                if (!catFilterMatch) return@filter false

                if (normQ.isEmpty()) return@filter true

                val nameNorm = normalize(p.name)
                val descNorm = normalize(p.description)
                val catNameNorm = p.category?.let { normalize(categoryNames[it]) }

                val tokenMatch = tokens.any { t ->
                    nameNorm.contains(t) || (descNorm?.contains(t) == true) || (catNameNorm?.contains(t) == true)
                }

                val exactCatMatch = exactCategoryId != null && exactCategoryId == p.category

                tokenMatch || exactCatMatch
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialQuery = arguments?.getString("query")
        initialCategoryId = if (arguments?.containsKey("category_id") == true) arguments?.getInt("category_id") else null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        adapter = ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                intent.putExtra("product", product)
                startActivity(intent)
            },
            onResultsCountChanged = { count ->
                // Empty check handled by onProductsLoaded
            }
        )
        val glm = GridLayoutManager(requireContext(), 2)
        // Hacer que el header ocupe el ancho completo (span=2)
        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == 0) 2 else 1
            }
        }
        binding.recycler.layoutManager = glm
        binding.recycler.adapter = adapter
        initialCategoryId?.let { adapter.categoryIdFilter = it }

        binding.swipeRefresh.setOnRefreshListener { loadProducts() }
        binding.state.btnRetry.setOnClickListener { loadProducts() }

        // FAB logic removed as per new design

        val cm = CartManager(requireContext())
        // Badge logic removed from Fragment as it is now in Toolbar (Activity)
        cartPrefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> 
             // Optional: Update activity badge if needed
        }
        cm.registerListener(cartPrefsListener!!)

        StateUi.hide(binding.state)
        loadProducts()
        loadCategoriesMap()
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            StateUi.showLoading(binding.state)
            binding.state.tvEmpty.visibility = View.GONE 
        }
        // FAB visibility logic removed
        if (!loading) binding.swipeRefresh.isRefreshing = false
    }

    private fun showError(message: String) {
        StateUi.showError(binding.state, message)
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showEmpty() {
        val msg = when {
            adapter.categoryIdFilter != null && !initialQuery.isNullOrBlank() -> "Sin resultados en la categoría \"${initialQuery}\""
            adapter.categoryIdFilter != null -> "Sin resultados en esta categoría"
            !initialQuery.isNullOrBlank() -> "Sin resultados para \"${initialQuery}\""
            else -> null
        }
        if (msg != null) {
            binding.state.tvEmpty.text = msg
        }
        StateUi.showEmpty(binding.state)
        binding.swipeRefresh.isRefreshing = false
    }

    private fun hideError() {
        StateUi.hide(binding.state)
    }

    private fun loadProducts() {
        if (!isOnline()) {
            showError(getString(R.string.msg_products_error, "Sin conexión a Internet"))
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val authed = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { authed.getProducts() }
                
                onProductsLoaded(products)
                
            } catch (e: Exception) {
                val fallback = (e is HttpException && (e.code() == 401 || e.code() == 403))
                if (fallback) {
                    try {
                        val publicSvc = RetrofitClient.createProductServicePublic(requireContext())
                        val products = withContext(Dispatchers.IO) { publicSvc.getProducts() }
                        onProductsLoaded(products)
                    } catch (e2: Exception) {
                        showError(getString(R.string.msg_products_error, NetworkError.message(e2)))
                    }
                } else {
                    val msg = if (e is UnknownHostException) "Sin conexión o host no resolvible" else NetworkError.message(e)
                    showError(getString(R.string.msg_products_error, msg))
                }
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun onProductsLoaded(list: List<Product>) {
        adapter.initialQuery = initialQuery
        adapter.setProducts(list)
        
        val filteredCount = adapter.itemCount - 1 
        
        if (filteredCount <= 0) {
            showEmpty()
        } else {
            hideError()
        }
    }

    private fun loadCategoriesMap() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createCategoryService(requireContext())
                val categories = withContext(Dispatchers.IO) { service.getCategories() }
                val map = categories.associate { it.id to it.name }
                adapter.setCategoryNames(map)
            } catch (_: Exception) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            val cm = CartManager(requireContext())
            cartPrefsListener?.let { cm.unregisterListener(it) }
            cartPrefsListener = null
        } catch (_: Exception) {}
        _binding = null
    }

    private fun isOnline(): Boolean {
        val cm = requireContext().getSystemService(ConnectivityManager::class.java)
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
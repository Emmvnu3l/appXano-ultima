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
                // Check adapter items directly instead of using filter count only, to avoid premature empty state
                // But wait, the callback tells us the filtered count.
                // The issue is that initially original list is empty, so filter is empty, so it shows empty.
                // We need to know if we are loading.
                // adapter.itemCount includes header, so count might be filtered size.
                
                // The user wants "Sin resultados" only when loading is finished and result is truly empty.
                // We handle loading state separately.
                // During loading, error/empty/content views should be hidden or showing loading.
                
                // We will defer showing empty state until loading is false.
                // But here we are in a callback from adapter.
                // The adapter filter runs synchronously usually when data is set.
                
                // Let's trust that when data is loaded, we call setProducts, which triggers filter, which triggers this callback.
                // We should just ensure we don't show empty while loading.
                // See onProductsLoaded.
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

        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setOnClickListener { NavigationHelper.openCart(requireContext()) }

        val cm = CartManager(requireContext())
        val updateBadge = {
            val count = cm.getItems().values.sum()
            if (count > 0) {
                binding.tvCartBadge.visibility = View.VISIBLE
                binding.tvCartBadge.text = count.toString()
            } else {
                binding.tvCartBadge.visibility = View.GONE
            }
        }
        updateBadge()
        cartPrefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> updateBadge() }
        cm.registerListener(cartPrefsListener!!)

        // Initial state: Hide empty message until we know for sure
        StateUi.hide(binding.state)
        loadProducts()
        loadCategoriesMap()
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            StateUi.showLoading(binding.state)
            // Ensure empty message is hidden while loading
            binding.state.tvEmpty.visibility = View.GONE 
        } else {
            // When not loading, StateUi.hide() or StateUi.showEmpty() will be called by onProductsLoaded
            // But let's just hide the loading indicator here.
            // Note: StateUi.hide() hides everything including empty message, so be careful.
            // We want to hide the loading view specifically.
            // Assuming StateUi.showLoading makes loading visible and others gone.
            // We will handle the final state in onProductsLoaded.
        }
        binding.fabAdd.visibility = if (loading) View.GONE else View.VISIBLE
        if (!loading) binding.swipeRefresh.isRefreshing = false
    }

    private fun showError(message: String) {
        StateUi.showError(binding.state, message)
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showEmpty() {
        // Only show empty if we are not loading. But showEmpty is usually called after loading.
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
                Log.d("ProductsLoad", "Auth returned ${products.size} products")
                if (products.isEmpty()) {
                    try {
                        val publicSvc = RetrofitClient.createProductServicePublic(requireContext())
                        val publicProducts = withContext(Dispatchers.IO) { publicSvc.getProducts() }
                        Log.d("ProductsLoad", "Public fallback after empty auth returned ${publicProducts.size} products")
                        onProductsLoaded(publicProducts)
                    } catch (e2: Exception) {
                        Log.e("ProductsLoad", "Public fallback after empty auth failed: ${NetworkError.message(e2)}")
                        onProductsLoaded(products)
                    }
                } else {
                    onProductsLoaded(products)
                }
            } catch (e: Exception) {
                val fallback = (e is HttpException && (e.code() == 401 || e.code() == 403))
                if (fallback) {
                    try {
                        val publicSvc = RetrofitClient.createProductServicePublic(requireContext())
                        val products = withContext(Dispatchers.IO) { publicSvc.getProducts() }
                        Log.d("ProductsLoad", "Public fallback after auth error returned ${products.size} products")
                        onProductsLoaded(products)
                    } catch (e2: Exception) {
                        Log.e("ProductsLoad", "Public fallback failed: ${NetworkError.message(e2)}")
                        showError(getString(R.string.msg_products_error, NetworkError.message(e2)))
                    }
                } else {
                    val msg = if (e is UnknownHostException) "Sin conexión o host no resolvible" else NetworkError.message(e)
                    Log.e("ProductsLoad", "Auth request failed: $msg")
                    showError(getString(R.string.msg_products_error, msg))
                }
            } finally {
                // We don't call setLoading(false) here generically because onProductsLoaded handles UI state.
                // But if error happened, showError handled it.
                // If success, onProductsLoaded handled it.
                // However, to be safe, we can ensure swipeRefresh is stopped.
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun onProductsLoaded(list: List<Product>) {
        adapter.initialQuery = initialQuery
        adapter.setProducts(list)
        
        // Now we determine if it's empty based on the FILTERED list, not just the raw list.
        // adapter.setProducts triggers the filter logic.
        // We can check the adapter item count (minus header).
        val filteredCount = adapter.itemCount - 1 // -1 for header
        
        if (filteredCount <= 0) {
            showEmpty()
        } else {
            hideError()
        }
        // Stop loading indicator (StateUi loading view)
        // hideError() basically does StateUi.hide(), which is what we want if not empty.
        // If empty, showEmpty() does StateUi.showEmpty().
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
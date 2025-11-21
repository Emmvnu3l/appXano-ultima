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
    // Comentario: query inicial opcional para filtrar productos por nombre/descr.
    private var initialQuery: String? = null
    private var cartPrefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    companion object {
        // Comentario: permite crear el fragment con un query predefinido.
        fun newInstance(query: String?): ProductsFragment {
            val f = ProductsFragment()
            val args = Bundle()
            args.putString("query", query)
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Comentario: recupera el query pasado por argumentos.
        initialQuery = arguments?.getString("query")
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
                if (count == 0) {
                    showEmpty()
                } else {
                    hideError()
                }
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

        loadProducts()
    }

    private fun setLoading(loading: Boolean) {
        if (loading) StateUi.showLoading(binding.state) else StateUi.hide(binding.state)
        binding.fabAdd.visibility = if (loading) View.GONE else View.VISIBLE
        if (!loading) binding.swipeRefresh.isRefreshing = false
    }

    private fun showError(message: String) {
        StateUi.showError(binding.state, message)
        binding.swipeRefresh.isRefreshing = false
    }

    private fun showEmpty() {
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
                setLoading(false)
            }
        }
    }

    private fun onProductsLoaded(list: List<Product>) {
        // Pasamos el query inicial al adapter para que lo aplique y muestre en el header
        adapter.initialQuery = initialQuery
        // Filtrado previo por categoría si viene en initialQuery y no es una búsqueda normal
        // Asumimos que si initialQuery coincide con una categoría, filtramos por category_id o similar, 
        // pero como Product no tiene category_id explícito fácil, filtramos por category string si existe.
        // Si initialQuery no es null, ProductAdapter ya lo filtra por texto.
        // Para soportar filtrado exacto de categoría, idealmente Product debería tener un campo category_id.
        // Aquí confiamos en el filtro de texto del adapter por ahora.
        
        adapter.setProducts(list)
        if (list.isEmpty()) {
            showEmpty()
        } else {
            hideError()
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
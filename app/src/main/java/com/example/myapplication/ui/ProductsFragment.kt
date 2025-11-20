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

class ProductsFragment : Fragment() {
    private var _binding: FragmentProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductAdapter
    // Comentario: query inicial opcional para filtrar productos por nombre/descr.
    private var initialQuery: String? = null

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

        val tm = TokenManager(requireContext())
        if (tm.isAdmin()) {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener { NavigationHelper.openAddProduct(requireContext()) }
        } else {
            binding.fabAdd.visibility = View.VISIBLE
            binding.fabAdd.setOnClickListener { NavigationHelper.openCart(requireContext()) }
        }

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
        binding.state.tvError.visibility = View.GONE
    }

    private fun loadProducts() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val authed = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { authed.getProducts() }
                onProductsLoaded(products)
            } catch (e: Exception) {
                val fallback = (e is HttpException && e.code() == 401)
                if (fallback) {
                    try {
                        val publicSvc = RetrofitClient.createProductServicePublic(requireContext())
                        val products = withContext(Dispatchers.IO) { publicSvc.getProducts() }
                        onProductsLoaded(products)
                    } catch (e2: Exception) {
                        showError(getString(R.string.msg_products_error, NetworkError.message(e2)))
                    }
                } else {
                    showError(getString(R.string.msg_products_error, NetworkError.message(e)))
                }
            } finally {
                setLoading(false)
            }
        }
    }

    private fun onProductsLoaded(list: List<Product>) {
        // Pasamos el query inicial al adapter para que lo aplique y muestre en el header
        adapter.initialQuery = initialQuery
        adapter.setProducts(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
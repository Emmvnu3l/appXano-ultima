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
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.FragmentProductsBinding
import com.example.myapplication.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    binding.tvError.text = "Sin resultados"
                    binding.tvError.visibility = View.VISIBLE
                } else {
                    binding.tvError.visibility = View.GONE
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
        binding.btnRetry.setOnClickListener { loadProducts() }

        val tm = TokenManager(requireContext())
        // Modo testeo: mostrar siempre el FAB
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setOnClickListener {
            NavigationHelper.openAddProduct(requireContext())
        }

        loadProducts()
    }

    private fun setLoading(loading: Boolean) {
        // Mostrar solo el spinner centrado cuando loading=true; ocultar el contenido
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.swipeRefresh.visibility = if (loading) View.GONE else View.VISIBLE
        binding.fabAdd.visibility = if (loading) View.GONE else View.VISIBLE
        // Ocultar mensajes de error al iniciar una nueva carga
        binding.tvError.visibility = View.GONE
        if (!loading) binding.swipeRefresh.isRefreshing = false
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun loadProducts() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { service.getProducts() }
                onProductsLoaded(products)
            } catch (e: Exception) {
                showError("Error cargando productos: ${e.message}")
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
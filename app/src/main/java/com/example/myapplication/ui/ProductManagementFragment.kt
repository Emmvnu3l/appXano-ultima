package com.example.myapplication.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentProductManagementBinding
import com.example.myapplication.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductManagementFragment : Fragment() {
    private var _binding: FragmentProductManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Manejo de la Toolbar
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = ProductManagementAdapter(
            onEditClick = { product ->
                // Reutilizamos EditProductFragment
                val fragment = EditProductFragment.newInstance(product)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { product ->
                confirmDelete(product)
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.fabAddProduct.setOnClickListener {
            // Usamos AddProductFragment existente
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddProductFragment())
                .addToBackStack(null)
                .commit()
        }

        loadProducts()
    }

    private fun loadProducts() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { service.getProducts() }
                adapter.submitList(products)
                binding.tvEmpty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error cargando productos: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar producto")
            .setMessage("¿Estás seguro de eliminar '${product.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteProduct(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProduct(id: Int) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                withContext(Dispatchers.IO) { service.deleteProduct(id) }
                Toast.makeText(requireContext(), "Producto eliminado", Toast.LENGTH_SHORT).show()
                loadProducts()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error eliminando: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
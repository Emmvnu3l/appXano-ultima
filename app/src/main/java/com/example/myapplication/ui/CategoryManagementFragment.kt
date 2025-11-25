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
import com.example.myapplication.api.NetworkError
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentCategoryManagementBinding
import com.example.myapplication.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoryManagementFragment : Fragment() {
    private var _binding: FragmentCategoryManagementBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CategoryManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = CategoryManagementAdapter(
            onEditClick = { category ->
                // Navegar a edición (podría reutilizar CreateCategoryFragment con argumentos)
                Toast.makeText(requireContext(), "Editar no implementado aún", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { category ->
                confirmDelete(category)
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.fabAddCategory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CreateCategoryFragment())
                .addToBackStack(null)
                .commit()
        }

        loadCategories()
    }

    private fun loadCategories() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createCategoryService(requireContext())
                val categories = withContext(Dispatchers.IO) { service.getCategories() }
                adapter.submitList(categories)
                binding.tvEmpty.visibility = if (categories.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error cargando categorías: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmDelete(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar categoría")
            .setMessage("¿Estás seguro de eliminar '${category.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCategory(category.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCategory(id: Int) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createCategoryService(requireContext())
                withContext(Dispatchers.IO) { service.deleteCategory(id) }
                Toast.makeText(requireContext(), "Categoría eliminada", Toast.LENGTH_SHORT).show()
                loadCategories()
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
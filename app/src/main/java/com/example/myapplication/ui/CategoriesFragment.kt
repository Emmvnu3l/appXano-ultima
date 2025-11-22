package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.R
import com.example.myapplication.api.NetworkError
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentCategoriesBinding
import com.example.myapplication.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CategoriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CategoriesAdapter { category ->
            val fragment = ProductsFragment.newInstance(category.name, category.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            // Opcional: Actualizar título o selección en NavigationView si es necesario
        }

        binding.recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadCategories() }

        loadCategories()
    }

    private fun loadCategories() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createCategoryService(requireContext())
                val categories = withContext(Dispatchers.IO) { service.getCategories() }
                
                if (categories.isEmpty()) {
                    StateUi.showEmpty(binding.state)
                } else {
                    StateUi.hide(binding.state)
                    adapter.submitList(categories)
                }
            } catch (e: Exception) {
                StateUi.showError(binding.state, NetworkError.message(e))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (adapter.itemCount == 0) {
             if (isLoading) StateUi.showLoading(binding.state) else StateUi.hide(binding.state)
             binding.swipeRefresh.isRefreshing = false
        } else {
             binding.swipeRefresh.isRefreshing = isLoading
             StateUi.hide(binding.state)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
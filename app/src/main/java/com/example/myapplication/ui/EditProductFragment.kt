package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentEditProductBinding
import com.example.myapplication.model.Product
import com.example.myapplication.model.UpdateProductRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProductFragment : Fragment() {
    private var _binding: FragmentEditProductBinding? = null
    private val binding get() = _binding!!
    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        product = arguments?.getSerializable("product") as? Product
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        val p = product
        if (p != null) {
            binding.etName.setText(p.name)
            binding.etDescription.setText(p.description ?: "")
            binding.etPrice.setText(p.price.toString())
            binding.etStock.setText("")
            binding.etBrand.setText("")
        }

        binding.btnSave.setOnClickListener { submitUpdate() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        setLoading(false)
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.btnDelete.isEnabled = !loading
    }

    private fun submitUpdate() {
        val p = product ?: return
        val req = UpdateProductRequest(
            name = binding.etName.text?.toString()?.trim(),
            description = binding.etDescription.text?.toString()?.trim(),
            price = binding.etPrice.text?.toString()?.toDoubleOrNull(),
            stock = binding.etStock.text?.toString()?.toIntOrNull(),
            brand = binding.etBrand.text?.toString()?.trim(),
            category = null
        )
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                withContext(Dispatchers.IO) { service.updateProduct(p.id, req) }
                activity?.onBackPressedDispatcher?.onBackPressed()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar")
            .setMessage("¿Eliminar producto?")
            .setPositiveButton("Sí") { _, _ -> deleteProduct() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteProduct() {
        val p = product ?: return
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                withContext(Dispatchers.IO) { service.deleteProduct(p.id) }
                activity?.onBackPressedDispatcher?.onBackPressed()
            } finally {
                setLoading(false)
            }
        }
    }

    companion object {
        fun newInstance(product: Product): EditProductFragment {
            val f = EditProductFragment()
            val args = Bundle()
            args.putSerializable("product", product)
            f.arguments = args
            return f
        }
    }
}
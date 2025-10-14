package com.example.myapplication.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentAddProductBinding
import com.example.myapplication.model.CreateProductRequest
import com.example.myapplication.model.ProductImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class AddProductFragment : Fragment() {
    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!
    private var selectedUris: List<Uri> = emptyList()

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedUris = uris ?: emptyList()
        binding.tvImagesStatus.text = "Imágenes seleccionadas: ${selectedUris.size}"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickImages.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.btnCreate.setOnClickListener {
            createProduct()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !loading
    }

    private fun createProduct() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim()
        val priceStr = binding.etPrice.text?.toString()?.trim().orEmpty()
        val price = priceStr.toDoubleOrNull()
        if (name.isEmpty() || price == null) {
            Toast.makeText(requireContext(), "Nombre y precio son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // 1) Subir imágenes si hay
                val uploadService = RetrofitClient.createUploadService(requireContext())
                val uploaded: MutableList<ProductImage> = mutableListOf()
                for (uri in selectedUris) {
                    val part = makeImagePart(uri)
                    val img = withContext(Dispatchers.IO) { uploadService.uploadImage(part) }
                    uploaded.add(img)
                }

                // 2) Crear producto
                val productService = RetrofitClient.createProductService(requireContext())
                val req = CreateProductRequest(name = name, description = description, price = price, images = uploaded)
                withContext(Dispatchers.IO) { productService.createProduct(req) }
                Toast.makeText(requireContext(), "Producto creado", Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error creando producto: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun clearForm() {
        binding.etName.setText("")
        binding.etDescription.setText("")
        binding.etPrice.setText("")
        selectedUris = emptyList()
        binding.tvImagesStatus.text = "Sin imágenes"
    }

    private fun makeImagePart(uri: Uri): MultipartBody.Part {
        val contentResolver = requireContext().contentResolver
        val mime = contentResolver.getType(uri) ?: "image/*"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val rb: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val filename = uri.lastPathSegment ?: "image.jpg"
        return MultipartBody.Part.createFormData("image", filename, rb)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
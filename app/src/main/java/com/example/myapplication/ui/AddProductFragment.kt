package com.example.myapplication.ui

import android.net.Uri
import android.webkit.MimeTypeMap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
 
import com.example.myapplication.api.TokenManager
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentAddProductBinding
import com.example.myapplication.model.CreateProductFullRequest
import com.example.myapplication.model.Category
import com.example.myapplication.model.ProductImage
import com.example.myapplication.model.ImagePayload
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
    private lateinit var tokenManager: TokenManager
    private var categories: List<Category> = emptyList()

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        // Selector de múltiples imágenes: guarda los Uri seleccionados y actualiza el estado en UI.
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
        tokenManager = TokenManager(requireContext())
        // Ocultar el toolbar propio del fragmento para evitar doble flecha
        binding.toolbar.visibility = View.GONE
        setupCategory()


        binding.btnPickImages.setOnClickListener {
            // Abre el selector de imágenes del sistema. Filtra por tipo MIME 'image/*'.
            pickImages.launch("image/*")
        }

        binding.btnCreate.setOnClickListener {
            // 1) Sube cada imagen como Multipart.
            // 2) Convierte la respuesta ProductImage a ImagePayload.
            // 3) Envía CreateProductFullRequest al endpoint POST /product.
            createProduct()
        }
    }

    private fun setupCategory() {
        // Siempre visible; cargamos categorías desde la API y habilitamos al finalizar.
        binding.spCategory.visibility = View.VISIBLE
        binding.spCategory.isEnabled = false
        binding.btnCreate.isEnabled = false
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createCategoryService(requireContext())
                val list = withContext(Dispatchers.IO) { service.getCategories() }
                categories = list
                val names = list.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spCategory.adapter = adapter
                binding.spCategory.isEnabled = true
                binding.btnCreate.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error cargando categorías: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        val stock = binding.etStock.text?.toString()?.trim()?.toIntOrNull()
        val brand = binding.etBrand.text?.toString()?.trim()
        val categoryId = categories.getOrNull(binding.spCategory.selectedItemPosition)?.id
        if (name.isEmpty() || price == null || categoryId == null) {
            Toast.makeText(requireContext(), "Nombre, precio y categoría son obligatorios", Toast.LENGTH_SHORT).show()
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
                val payloads: List<ImagePayload> = uploaded.map { toPayload(it) }
                val req = CreateProductFullRequest(
                    name = name,
                    description = description,
                    price = price!!,
                    stock = stock,
                    brand = brand,
                    category = categoryId,
                    images = payloads
                )
                withContext(Dispatchers.IO) { productService.createProductFull(req) }
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
        binding.etStock.setText("")
        binding.etBrand.setText("")
        selectedUris = emptyList()
        binding.tvImagesStatus.text = "Sin imágenes"
        if (binding.spCategory.visibility == View.VISIBLE && binding.spCategory.adapter?.count ?: 0 > 0) {
            binding.spCategory.setSelection(0)
        }
    }

    private fun makeImagePart(uri: Uri): MultipartBody.Part {
        val cr = requireContext().contentResolver
        // Obtiene MIME real si existe; por defecto usa JPEG
        val mime = cr.getType(uri) ?: "image/jpeg"
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val rb: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        // Determina extensión desde MIME para que el backend acepte el archivo
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val filename = "image_${System.currentTimeMillis()}.$ext"
        return MultipartBody.Part.createFormData("content", filename, rb)
    }

    private fun toPayload(img: ProductImage): ImagePayload {
        // Transforma la respuesta de subida (ProductImage) en el payload que espera el endpoint de producto.
        // Incluye path, nombre, tipo y metadatos como tamaño y MIME.
        val path = img.path ?: img.url ?: ""
        val name = path.split('/').lastOrNull()
        return ImagePayload(
            access = "public",
            path = path,
            name = name,
            type = "image",
            size = img.size,
            mime = img.mime,
            meta = emptyMap()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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
import android.provider.OpenableColumns
import android.graphics.BitmapFactory
import org.json.JSONObject
 
import com.example.myapplication.api.TokenManager
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.FragmentAddProductBinding
import com.example.myapplication.model.CreateProductFullRequest
import com.example.myapplication.model.CreateProductBasicRequest
import com.example.myapplication.model.Category
import com.example.myapplication.model.ProductImage
import com.example.myapplication.model.ImagePayload
import com.example.myapplication.model.ProductImageCreateItem
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
        binding.tvImagesStatus.text = getString(com.example.myapplication.R.string.msg_images_selected, selectedUris.size)
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
        binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        tokenManager = TokenManager(requireContext())
        // Ocultar el toolbar propio del fragmento para evitar doble flecha
        binding.toolbar.visibility = View.GONE
        setupCategory()


        binding.btnPickImages.setOnClickListener {
            // Abre el selector de imágenes del sistema. Filtra por tipo MIME 'image/*'.
            pickImages.launch("image/*")
        }

        binding.btnCreate.setOnClickListener {
            // Nuevo flujo API v0.0.1: crear producto básico y luego asociar imágenes vía /product_image
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
                Toast.makeText(requireContext(), getString(com.example.myapplication.R.string.msg_categories_error, e.message ?: ""), Toast.LENGTH_LONG).show()
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
            Toast.makeText(requireContext(), getString(com.example.myapplication.R.string.msg_required_fields), Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // 1) Crear producto básico
                val productService = RetrofitClient.createProductService(requireContext())
                val basicReq = CreateProductBasicRequest(
                    name = name,
                    description = description,
                    price = price!!,
                    stock = stock,
                    brand = brand,
                    categoryId = categoryId
                )
                val created = withContext(Dispatchers.IO) { productService.createProductBasic(basicReq) }
                val productId = created.product?.id ?: created.productId ?: created.id

                // 2) Si hay imágenes seleccionadas, subir y asociar
                if (productId != null && productId > 0 && selectedUris.isNotEmpty()) {
                    val uploadService = RetrofitClient.createUploadService(requireContext())
                    val payloads = mutableListOf<ImagePayload>()
                    val forProductImageApi = mutableListOf<ProductImageCreateItem>()
                    for (uri in selectedUris) {
                        val part = makeImagePart(uri)
                        val img = withContext(Dispatchers.IO) { uploadService.uploadImage(part) }
                        val path = img.path ?: img.url ?: ""
                        if (path.isBlank()) continue
                        val nameImg = path.split('/').lastOrNull()
                        val displayName = queryDisplayName(uri) ?: nameImg
                        val bounds = queryImageBounds(uri)
                        val metaMap = mutableMapOf<String, Any>()
                        if (displayName != null) metaMap["name"] = displayName
                        metaMap["source"] = "android"
                        metaMap["size"] = img.size ?: 0
                        metaMap["mime"] = img.mime ?: ""
                        if (bounds != null) {
                            metaMap["width"] = bounds.first
                            metaMap["height"] = bounds.second
                        }
                        payloads.add(
                            ImagePayload(
                                access = "public",
                                path = path,
                                name = displayName,
                                type = "image",
                                size = img.size,
                                mime = img.mime,
                                meta = metaMap
                            )
                        )
                        forProductImageApi.add(
                            ProductImageCreateItem(
                                productId = productId,
                                access = "public",
                                path = path,
                                name = displayName,
                                type = "image",
                                size = img.size,
                                mime = img.mime,
                                meta = JSONObject(metaMap as Map<*, *>).toString()
                            )
                        )
                    }

                    // Preferir PATCH /product con array de imágenes en el atributo del producto
                    try {
                        val req = com.example.myapplication.model.UpdateProductRequest(
                            name = null,
                            description = null,
                            price = null,
                            stock = null,
                            brand = null,
                            categoryId = null,
                            img = payloads
                        )
                        withContext(Dispatchers.IO) { productService.patchProduct(productId, req) }
                    } catch (ePatch: Exception) {
                        // Fallback a endpoints /product_image
                        try {
                            val imageService = RetrofitClient.createProductImageService(requireContext())
                            for (item in forProductImageApi) {
                                withContext(Dispatchers.IO) { imageService.createProductImage(item) }
                            }
                        } catch (e1: Exception) {
                            try {
                                val imageService = RetrofitClient.createProductImageService(requireContext())
                                withContext(Dispatchers.IO) { imageService.createProductImagesPlural(forProductImageApi) }
                            } catch (e2: Exception) {
                                // Último fallback: crear producto con imágenes embebidas
                                val reqFull = CreateProductFullRequest(
                                    name = name,
                                    description = description,
                                    price = price!!,
                                    stock = stock,
                                    brand = brand,
                                    categoryId = categoryId,
                                    img = payloads
                                )
                                withContext(Dispatchers.IO) { productService.createProductFull(reqFull) }
                            }
                        }
                    }
                }

                Toast.makeText(requireContext(), getString(com.example.myapplication.R.string.msg_product_created), Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(com.example.myapplication.R.string.msg_product_create_error, e.message ?: ""), Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cr = requireContext().contentResolver
        val c = cr.query(uri, null, null, null, null)
        return c?.use {
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
        }
    }

    private fun queryImageBounds(uri: Uri): Pair<Int, Int>? {
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        val isr = requireContext().contentResolver.openInputStream(uri) ?: return null
        isr.use { BitmapFactory.decodeStream(it, null, opts) }
        if (opts.outWidth > 0 && opts.outHeight > 0) return Pair(opts.outWidth, opts.outHeight)
        return null
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

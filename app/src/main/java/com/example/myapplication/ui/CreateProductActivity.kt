package com.example.myapplication.ui

import android.net.Uri
import android.webkit.MimeTypeMap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityCreateProductBinding
import com.example.myapplication.model.CreateProductFullRequest
import com.example.myapplication.model.ImagePayload
import com.example.myapplication.model.ProductImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class CreateProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateProductBinding
    private lateinit var tokenManager: TokenManager
    private var selectedUris: List<Uri> = emptyList()

    private val categories = listOf(
        "15% de descuento",
        "50% de descuento",
        "perecibles",
        "no perecibles",
        "congelados"
    )

    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedUris = uris ?: emptyList()
        binding.tvImagesStatus.text = "Imágenes seleccionadas: ${selectedUris.size}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opción 3: desactivar edge-to-edge para evitar solapamientos con barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityCreateProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupCategory()

        binding.btnPickImages.setOnClickListener { pickImages.launch("image/*") }
        binding.btnCreate.setOnClickListener { submitCreate() }
    }

    private fun setupCategory() {
        val isAdmin = tokenManager.isAdmin()
        binding.spCategory.visibility = if (isAdmin) View.VISIBLE else View.GONE
        if (isAdmin) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spCategory.adapter = adapter
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !loading
    }

    private fun submitCreate() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim()
        val price = binding.etPrice.text?.toString()?.trim()?.toDoubleOrNull()
        val stock = binding.etStock.text?.toString()?.trim()?.toIntOrNull()
        val brand = binding.etBrand.text?.toString()?.trim()
        val category = if (binding.spCategory.visibility == View.VISIBLE && binding.spCategory.selectedItem != null) {
            binding.spCategory.selectedItem.toString()
        } else null

        if (name.isEmpty() || price == null) {
            Toast.makeText(this, "Nombre y precio son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val uploadService = RetrofitClient.createUploadService(this@CreateProductActivity)
                val uploadedPayloads: MutableList<ImagePayload> = mutableListOf()
                for (uri in selectedUris) {
                    val part = makeImagePart(uri)
                    val img = withContext(Dispatchers.IO) { uploadService.uploadImage(part) }
                    uploadedPayloads.add(toPayload(img))
                }

                val productService = RetrofitClient.createProductService(this@CreateProductActivity)
                val req = CreateProductFullRequest(
                    name = name,
                    description = description,
                    price = price,
                    stock = stock,
                    brand = brand,
                    category = category,
                    images = uploadedPayloads
                )
                withContext(Dispatchers.IO) { productService.createProductFull(req) }
                Toast.makeText(this@CreateProductActivity, "Producto creado", Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (e: Exception) {
                Toast.makeText(this@CreateProductActivity, "Error creando producto: ${e.message}", Toast.LENGTH_LONG).show()
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
        val cr = contentResolver
        // Obtiene MIME real si existe; por defecto usa JPEG
        val mime = cr.getType(uri) ?: "image/jpeg"
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val rb: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        // Determina extensión desde MIME (jpg/png/webp, etc.)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val filename = "image_${System.currentTimeMillis()}.$ext"
        return MultipartBody.Part.createFormData("content", filename, rb)
    }

    private fun toPayload(img: ProductImage): ImagePayload {
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
}
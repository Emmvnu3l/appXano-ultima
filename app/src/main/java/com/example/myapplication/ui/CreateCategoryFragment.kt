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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.FragmentCreateCategoryBinding
import com.example.myapplication.model.CategoryCreateRequest
import com.example.myapplication.model.ImagePayload
import com.example.myapplication.model.ProductImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class CreateCategoryFragment : Fragment() {
    private var _binding: FragmentCreateCategoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager

    private var selectedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        val msg = if (uri != null) "Imagen seleccionada" else "Sin imagen"
        binding.tvImageStatus.text = msg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = TokenManager(requireContext())
        // Ocultar el toolbar propio del fragmento para evitar doble barra.
        binding.toolbar.visibility = View.GONE

        binding.btnPickImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnCreate.setOnClickListener {
            createCategory()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !loading
    }

    private fun createCategory() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val uri = selectedUri
        if (name.isEmpty() || description.isEmpty() || uri == null) {
            Toast.makeText(requireContext(), "Nombre, descripción e imagen son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                // 1) Subir imagen
                val uploadService = RetrofitClient.createUploadService(requireContext())
                val part = makeImagePart(uri)
                val uploaded: ProductImage = withContext(Dispatchers.IO) { uploadService.uploadImage(part) }

                // 2) Crear categoría
                val payload: ImagePayload = toPayload(uploaded)
                val req = CategoryCreateRequest(
                    name = name,
                    description = description,
                    image = payload
                )
                val service = RetrofitClient.createCategoryService(requireContext())
                withContext(Dispatchers.IO) { service.createCategory(req) }
                Toast.makeText(requireContext(), "Categoría creada", Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error creando categoría: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun clearForm() {
        binding.etName.setText("")
        binding.etDescription.setText("")
        selectedUri = null
        binding.tvImageStatus.text = "Sin imagen"
    }

    private fun makeImagePart(uri: Uri): MultipartBody.Part {
        val cr = requireContext().contentResolver
        val mime = cr.getType(uri) ?: "image/jpeg"
        val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val rb: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        val filename = "category_${System.currentTimeMillis()}.$ext"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
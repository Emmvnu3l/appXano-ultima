package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.myapplication.R
import com.example.myapplication.api.ApiConfig
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.databinding.ActivityProductDetailBinding
import com.example.myapplication.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri

class ProductDetailFragment : Fragment() {
    private var _binding: ActivityProductDetailBinding? = null
    private val binding get() = _binding!!
    private var quantity: Int = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tm = TokenManager(requireContext())
        var isAdmin = tm.isAdmin()

        if (tm.getRole().isNullOrBlank()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val me = withContext(Dispatchers.IO) { RetrofitClient.createAuthServiceAuthenticated(requireContext()).me() }
                    tm.setRole(me.role)
                    isAdmin = (me.role == "admin")
                    applyRoleUi(isAdmin)
                } catch (_: Exception) { }
            }
        }

        binding.toolbar.visibility = View.GONE

        val product = arguments?.getSerializable("product") as? Product
        if (product != null) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "${'$'}${product.price}"
            binding.tvDescription.text = product.description ?: ""
            val urlsRaw = product.img?.mapNotNull { it.url ?: it.path }.orEmpty()
            val urls = urlsRaw.mapNotNull { sanitizeImageUrl(it) }
            val cover = urls.firstOrNull()
            if (cover != null) {
                binding.ivImage.load(cover) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_gallery)
                    allowHardware(false)
                    memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    diskCachePolicy(coil.request.CachePolicy.ENABLED)
                }
            } else {
                binding.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            binding.ivImage.setOnClickListener {
                if (urls.isNotEmpty()) {
                    ImageViewerDialogFragment.newInstance(urls, 0)
                        .show(parentFragmentManager, "image_viewer")
                }
            }
        }

        quantity = 1
        binding.tvQuantity.text = quantity.toString()

        binding.btnMinus.setOnClickListener {
            val next = (quantity - 1).coerceAtLeast(1)
            quantity = next
            binding.tvQuantity.text = next.toString()
        }
        binding.btnPlus.setOnClickListener {
            val next = quantity + 1
            quantity = next
            binding.tvQuantity.text = next.toString()
        }

        binding.btnAddToCart.setOnClickListener {
            val cm = CartManager(requireContext())
            val q = quantity
            val product = arguments?.getSerializable("product") as? Product
            if (product != null) cm.add(product.id, q)
        }

        var expanded = false
        binding.tvMore.setOnClickListener {
            expanded = !expanded
            if (expanded) {
                binding.tvDescription.maxLines = Integer.MAX_VALUE
                binding.tvMore.text = getString(R.string.label_less)
            } else {
                binding.tvDescription.maxLines = 3
                binding.tvMore.text = getString(R.string.label_more)
            }
        }

        applyRoleUi(isAdmin)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyRoleUi(isAdmin: Boolean) {
        if (isAdmin) {
            binding.fabEdit.visibility = View.GONE
            binding.btnEditFixed.visibility = View.VISIBLE
            binding.btnEditFixed.setOnClickListener {
                val p = arguments?.getSerializable("product") as? Product
                if (p != null) NavigationHelper.openEditProduct(requireActivity(), p)
            }
        } else {
            binding.fabEdit.visibility = View.GONE
            binding.btnEditFixed.visibility = View.GONE
        }
    }

    private fun sanitizeImageUrl(s: String?): String? {
        if (s.isNullOrBlank()) return null
        var u = s.trim()
        u = u.replace("`", "").replace("\"", "")
        u = u.replace("\n", "").replace("\r", "").replace("\t", "")
        if (u.startsWith("/")) {
            val base = ApiConfig.storeBaseUrl
            val parsed = Uri.parse(base)
            val origin = (parsed.scheme ?: "https") + "://" + (parsed.host ?: "")
            u = origin + u
        }
        if (!u.startsWith("http")) {
            u = "https://" + u.trimStart('/')
        }
        u = u.replace(" ", "%20")
        return u
    }

    companion object {
        fun newInstance(product: Product): ProductDetailFragment {
            val f = ProductDetailFragment()
            val b = Bundle()
            b.putSerializable("product", product)
            f.arguments = b
            return f
        }
    }
}

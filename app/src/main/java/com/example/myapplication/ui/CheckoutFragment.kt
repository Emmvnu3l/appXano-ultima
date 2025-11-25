package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.TokenManager
import com.example.myapplication.api.NetworkError
import com.example.myapplication.databinding.ActivityCheckoutBinding
import com.example.myapplication.model.CreateOrderItem
import com.example.myapplication.model.CreateOrderRequest
import com.example.myapplication.model.ImagePayload
import com.example.myapplication.model.Order
import com.example.myapplication.model.UpdateOrderStatusRequest
import com.example.myapplication.model.UpdateProductRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutFragment : Fragment() {
    private var _binding: ActivityCheckoutBinding? = null
    private val binding get() = _binding!!
    private var currentOrder: Order? = null
    private lateinit var cartManager: CartManager
    private var couponPercentage: Double = 0.0
    private var validCouponCode: String = "1xssd2"
    private var validCouponExpiresYear: Int = 2026
    private lateinit var etCoupon: android.widget.EditText
    private lateinit var btnApplyCoupon: android.widget.Button
    private lateinit var tvCouponFeedback: android.widget.TextView
    private lateinit var tvTotalBefore: android.widget.TextView
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        renderCartSummary()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.visibility = View.GONE
        binding.toolbar.findViewById<View>(R.id.actionCart)?.visibility = View.GONE

        cartManager = CartManager(requireContext())
        etCoupon = binding.root.findViewById(R.id.etCoupon)
        btnApplyCoupon = binding.root.findViewById(R.id.btnApplyCoupon)
        tvCouponFeedback = binding.root.findViewById(R.id.tvCouponFeedback)
        tvTotalBefore = binding.root.findViewById(R.id.tvTotalBefore)
        btnApplyCoupon.isEnabled = false
        etCoupon.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val code = s?.toString()?.trim() ?: ""
                val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val valid = code.equals(validCouponCode, ignoreCase = true) && nowYear <= validCouponExpiresYear
                btnApplyCoupon.isEnabled = valid
                tvCouponFeedback.text = if (valid) "Cupón válido (10%)" else "Cupón inválido o expirado"
                if (!valid && couponPercentage > 0.0) {
                    couponPercentage = 0.0
                    renderCartSummary()
                }
            }
        })
        btnApplyCoupon.setOnClickListener {
            val code = etCoupon.text?.toString()?.trim() ?: ""
            val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val valid = code.equals(validCouponCode, ignoreCase = true) && nowYear <= validCouponExpiresYear
            if (valid) {
                couponPercentage = 0.10
                renderCartSummary()
            } else {
                couponPercentage = 0.0
                renderCartSummary()
            }
        }
        renderCartSummary()

        binding.btnPay.setOnClickListener { createPendingOrder() }
        binding.rgDelivery.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbDelivery) {
                loadUserAddress()
            } else {
                binding.tvShippingAddress.visibility = View.GONE
            }
        }
        binding.btnFinish.setOnClickListener { finalizeOrder() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cartManager.unregisterListener(prefsListener)
        _binding = null
    }

    private fun renderCartSummary() {
        val items = cartManager.getItems()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { service.getProducts() }
                val map = products.associateBy { it.id }
                val detail = buildString {
                    items.entries.forEach { (id, qty) ->
                        val p = map[id]
                        if (p != null) {
                            val line = p.price * qty
                            append(formatReceiptLine(p.name ?: "Producto", qty, line))
                            append("\n")
                        } else {
                            append(formatReceiptLine("Producto ${id}", qty, 0.0))
                            append("\n")
                        }
                    }
                }.trimEnd()
                binding.tvItems.text = detail.ifBlank { "Sin productos" }
                val pricing = computePricing(items, products)
                binding.tvSubtotal.text = formatCurrency(pricing.subtotal)
                binding.tvTax.text = formatCurrency(pricing.tax)
                binding.tvDiscount.text = formatCurrency(pricing.discount)
                tvTotalBefore.text = formatCurrency(pricing.subtotal)
                binding.tvTotal.text = formatCurrency(pricing.total)
                if (couponPercentage > 0.0) {
                    tvCouponFeedback.text = "Cupón aplicado: -${formatCurrency(pricing.discount)} (10%)"
                }
                binding.btnPay.isEnabled = items.isNotEmpty()
                cartManager.registerListener(prefsListener)
            } catch (_: Exception) {
                binding.tvTotal.text = "$0.00"
                binding.btnPay.isEnabled = false
            }
        }
    }

    private fun formatCurrency(value: Double): String {
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US)
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return "$${nf.format(value)}"
    }

    private data class Pricing(val subtotal: Double, val tax: Double, val discount: Double, val total: Double)

    private fun computePricing(items: Map<Int, Int>, products: List<com.example.myapplication.model.Product>): Pricing {
        val map = products.associateBy { it.id }
        var totalWithIva = 0.0
        var totalQty = 0
        for ((id, qty) in items) {
            val p = map[id] ?: continue
            totalWithIva += (p.price * qty)
            totalQty += qty
        }
        val taxRate = 0.19
        val tax = round2(totalWithIva * taxRate)
        val subtotalWithoutIva = round2(totalWithIva - tax)
        val discount = round2(subtotalWithoutIva * couponPercentage)
        val total = round2(subtotalWithoutIva - discount)
        return Pricing(subtotalWithoutIva, tax, discount, total)
    }

    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
    private fun formatReceiptLine(name: String, qty: Int, total: Double): String {
        val left = "$name x$qty"
        val right = formatCurrency(total)
        val target = 42
        val padCount = kotlin.math.max(2, target - left.length - right.length)
        val spaces = " ".repeat(padCount)
        return "$left$spaces$right"
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPay.isEnabled = !loading && binding.layoutDeliveryOptions.visibility == View.GONE
        binding.btnFinish.isEnabled = !loading
    }

    private fun createPendingOrder() {
        val itemsMap = cartManager.getItems()
        if (itemsMap.isEmpty()) {
            Toast.makeText(requireContext(), "Carrito vacío", Toast.LENGTH_SHORT).show()
            return
        }
        val items = itemsMap.entries.map { CreateOrderItem(it.key, it.value) }
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val productService = RetrofitClient.createProductService(requireContext())
                val products = withContext(Dispatchers.IO) { productService.getProducts() }
                val insufficient = itemsMap.entries.firstOrNull { (pid, qty) ->
                    val p = products.find { it.id == pid }
                    val stock = p?.stock ?: Int.MAX_VALUE
                    qty > stock
                }
                if (insufficient != null) {
                    Toast.makeText(requireContext(), "Stock insuficiente para producto ${insufficient.key}", Toast.LENGTH_LONG).show()
                    setLoading(false)
                    return@launch
                }
                val pricing = computePricing(itemsMap, products)
                val tm = TokenManager(requireContext())
                val uid = tm.getUserId()
                val orderService = RetrofitClient.createOrderService(requireContext())
                val backendCartId = cartManager.getBackendCartId()
                val order = if (backendCartId != null) {
                    val req = com.example.myapplication.model.CheckoutRequest(
                        cartId = backendCartId,
                        status = "pendiente",
                        userId = uid,
                        discountCodeId = uid
                    )
                    withContext(Dispatchers.IO) { orderService.checkout(req) }
                } else {
                    val request = CreateOrderRequest(items, pricing.total, status = "pendiente", userId = uid, discountCodeId = uid)
                    try {
                        withContext(Dispatchers.IO) { orderService.createOrder(request) }
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 400) {
                            val raw = mutableMapOf<String, Any>(
                                "total" to pricing.total,
                                "status" to "pendiente"
                            )
                            uid?.let { raw["user_id"] = it; raw["discount_code_id"] = it }
                            withContext(Dispatchers.IO) { orderService.createOrderRaw(raw) }
                        } else throw e
                    }
                }
                currentOrder = order
                cartManager.clear()
                Toast.makeText(requireContext(), "Pago confirmado", Toast.LENGTH_SHORT).show()
                binding.btnPay.visibility = View.GONE
                binding.layoutDeliveryOptions.visibility = View.GONE
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in_300,
                        R.anim.fade_out_300,
                        R.anim.fade_in_300,
                        R.anim.fade_out_300
                    )
                    .replace(R.id.fragmentContainer, PurchaseSuccessFragment())
                    .addToBackStack(null)
                    .commit()
                for ((pid, qty) in itemsMap) {
                    val p = products.find { it.id == pid } ?: continue
                    val newStock = ((p.stock ?: 0) - qty).coerceAtLeast(0)
                    val imagesPayload = (p.img ?: emptyList()).map { im ->
                        val path = im.path ?: (im.url ?: "")
                        val name = (im.path ?: im.url)?.substringAfterLast('/')
                        ImagePayload(access = "public", path = path, name = name, type = "image", size = im.size, mime = im.mime, meta = emptyMap())
                    }
                    val req = UpdateProductRequest(
                        name = p.name,
                        description = p.description,
                        price = p.price,
                        stock = newStock,
                        brand = p.brand,
                        categoryId = p.categoryId,
                        img = imagesPayload
                    )
                    try {
                        withContext(Dispatchers.IO) { productService.patchProduct(pid, req) }
                    } catch (_: Exception) { }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error creando orden: ${NetworkError.message(e)}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadUserAddress() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tm = TokenManager(requireContext())
                val service = RetrofitClient.createAuthServiceAuthenticated(requireContext())
                val me = withContext(Dispatchers.IO) { service.me() }
                val addr = me.shippingAddress
                binding.tvShippingAddress.text = if (!addr.isNullOrBlank()) "Dirección: $addr" else "No tiene dirección registrada (actualícela en su perfil)"
                binding.tvShippingAddress.visibility = View.VISIBLE
            } catch (_: Exception) {
                binding.tvShippingAddress.text = "Error cargando dirección"
                binding.tvShippingAddress.visibility = View.VISIBLE
            }
        }
    }

    private fun finalizeOrder() {
        val order = currentOrder ?: return
        val isDelivery = binding.rbDelivery.isChecked
        val isPickup = binding.rbPickup.isChecked
        if (!isDelivery && !isPickup) {
            Toast.makeText(requireContext(), "Seleccione un método de entrega", Toast.LENGTH_SHORT).show()
            return
        }
        val newStatus = if (isDelivery) "enviado" else "confirmada"
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.createOrderService(requireContext())
                withContext(Dispatchers.IO) { service.updateStatus(order.id, UpdateOrderStatusRequest(newStatus)) }
                Toast.makeText(requireContext(), "Orden finalizada", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error actualizando orden: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }
}

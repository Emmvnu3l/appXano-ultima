package com.example.myapplication.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.net.Uri
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.model.UpdateProductRequest
import com.example.myapplication.model.ImagePayload
import com.example.myapplication.databinding.ActivityCheckoutBinding
import com.example.myapplication.model.CreateOrderItem
import com.example.myapplication.model.CreateOrderRequest
import com.example.myapplication.model.Order
import com.example.myapplication.model.UpdateOrderStatusRequest
import com.example.myapplication.api.NetworkError
import com.example.myapplication.api.TokenManager
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckoutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCheckoutBinding
    private var currentOrder: Order? = null
    private lateinit var cartManager: CartManager
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        renderCartSummary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.findViewById<View>(R.id.actionCart)?.visibility = View.GONE

        cartManager = CartManager(this)
        renderCartSummary()

        binding.btnPay.setOnClickListener { createPendingOrder() }
        binding.rgDelivery.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == com.example.myapplication.R.id.rbDelivery) {
                loadUserAddress()
            } else {
                binding.tvShippingAddress.visibility = View.GONE
            }
        }
        
    }

    private fun renderCartSummary() {
        val items = cartManager.getItems()
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(this@CheckoutActivity)
                val products = withContext(Dispatchers.IO) { service.getProducts() }
                val map = products.associateBy { it.id }
                val detail = buildString {
                    items.entries.forEach { (id, qty) ->
                        val p = map[id]
                        if (p != null) {
                            val line = p.price * qty
                            append("• ${p.name} x${qty} = ")
                            append(formatCurrency(line))
                            append("\n")
                        } else {
                            append("• Producto ${id} x${qty}\n")
                        }
                    }
                }.trimEnd()
                binding.tvItems.text = detail.ifBlank { "Sin productos" }
                val pricing = computePricing(items, products)
                binding.tvSubtotal.text = formatCurrency(pricing.subtotal)
                binding.tvTax.text = formatCurrency(pricing.tax)
                binding.tvDiscount.text = formatCurrency(pricing.discount)
                binding.tvTotal.text = formatCurrency(pricing.total)
                binding.btnPay.isEnabled = items.isNotEmpty()
            } catch (e: Exception) {
                binding.tvTotal.text = "$0.00"
                binding.btnPay.isEnabled = false
            }
        }
    }

    private fun formatCurrency(value: Double): String = String.format("$%.2f", value)

    private data class Pricing(val subtotal: Double, val tax: Double, val discount: Double, val total: Double)

    private fun computePricing(items: Map<Int, Int>, products: List<com.example.myapplication.model.Product>): Pricing {
        val map = products.associateBy { it.id }
        var subtotal = 0.0
        var totalQty = 0
        for ((id, qty) in items) {
            val p = map[id] ?: continue
            subtotal += (p.price * qty)
            totalQty += qty
        }
        val taxRate = 0.19
        val tax = subtotal * taxRate
        val discountRate = if (totalQty >= 5) 0.05 else 0.0
        val discount = subtotal * discountRate
        val total = subtotal - discount + tax
        return Pricing(subtotal, tax, discount, total)
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPay.isEnabled = !loading
    }

    private fun createPendingOrder() {
        val cm = CartManager(this)
        val itemsMap = cm.getItems()
        if (itemsMap.isEmpty()) {
            Toast.makeText(this, "Carrito vacío", Toast.LENGTH_SHORT).show()
            return
        }
        val items = itemsMap.entries.map { CreateOrderItem(it.key, it.value) }
        setLoading(true)
        lifecycleScope.launch {
            try {
                val productService = RetrofitClient.createProductService(this@CheckoutActivity)
                val products = withContext(Dispatchers.IO) { productService.getProducts() }
                // Validación de stock disponible
                val insufficient = itemsMap.entries.firstOrNull { (pid, qty) ->
                    val p = products.find { it.id == pid }
                    val stock = p?.stock ?: Int.MAX_VALUE
                    qty > stock
                }
                if (insufficient != null) {
                    Toast.makeText(this@CheckoutActivity, "Stock insuficiente para producto ${insufficient.key}", Toast.LENGTH_LONG).show()
                    setLoading(false)
                    return@launch
                }
                val pricing = computePricing(itemsMap, products)
                val tm = TokenManager(this@CheckoutActivity)
                val uid = tm.getUserId()
                val orderService = RetrofitClient.createOrderService(this@CheckoutActivity)
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
                    } catch (e: HttpException) {
                        if (e.code() == 400) {
                            val raw = mutableMapOf<String, Any>(
                                "total" to pricing.total,
                                "status" to "pendiente"
                            )
                            uid?.let { raw["user_id"] = it; raw["discount_code_id"] = it }
                            try {
                                withContext(Dispatchers.IO) { orderService.createOrderRaw(raw) }
                            } catch (e2: HttpException) {
                                throw e2
                            }
                        } else throw e
                    }
                }
                currentOrder = order
                cm.clear()
                
                Toast.makeText(this@CheckoutActivity, "Pago confirmado", Toast.LENGTH_SHORT).show()
                
                // Update UI for delivery choice
                binding.btnPay.visibility = View.GONE
                binding.layoutDeliveryOptions.visibility = View.VISIBLE
                
                // Actualizar stock en segundo plano
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
                    } catch(e: Exception) {}
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutActivity, "Error creando orden: ${NetworkError.message(e)}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }
    
    private fun loadUserAddress() {
        lifecycleScope.launch {
            try {
                val tm = TokenManager(this@CheckoutActivity)
                val service = RetrofitClient.createAuthServiceAuthenticated(this@CheckoutActivity)
                val me = withContext(Dispatchers.IO) { service.me() }
                val addr = me.shippingAddress
                binding.tvShippingAddress.text = if (!addr.isNullOrBlank()) "Dirección: $addr" else "No tiene dirección registrada (actualícela en su perfil)"
                binding.tvShippingAddress.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.tvShippingAddress.text = "Error cargando dirección"
                binding.tvShippingAddress.visibility = View.VISIBLE
            }
        }
    }
    
    

    override fun onStart() {
        super.onStart()
        cartManager.registerListener(prefsListener)
    }

    override fun onStop() {
        super.onStop()
        cartManager.unregisterListener(prefsListener)
    }
}

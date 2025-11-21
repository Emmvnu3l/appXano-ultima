package com.example.myapplication.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import android.content.Intent
import android.net.Uri
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

        cartManager = CartManager(this)
        renderCartSummary()

        binding.btnPay.setOnClickListener { createPendingOrder() }
        binding.btnRequestShipping.setOnClickListener { requestShipping() }
    }

    private fun renderCartSummary() {
        val items = cartManager.getItems()
        binding.tvItems.text = items.entries.joinToString("\n") { (id, qty) -> "Producto $id x$qty" }
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createProductService(this@CheckoutActivity)
                val products = withContext(Dispatchers.IO) { service.getProducts() }
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
        binding.btnRequestShipping.isEnabled = !loading && currentOrder != null
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
                val pricing = computePricing(itemsMap, products)
                val tm = TokenManager(this@CheckoutActivity)
                val uid = tm.getUserId()
                val request = CreateOrderRequest(items, pricing.total, status = "pendiente", userId = uid, discountCodeId = uid)
                val orderService = RetrofitClient.createOrderService(this@CheckoutActivity)
                val order = try {
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
                currentOrder = order
                cm.clear()
                binding.tvItems.text = ""
                binding.tvSubtotal.text = formatCurrency(0.0)
                binding.tvTax.text = formatCurrency(0.0)
                binding.tvDiscount.text = formatCurrency(0.0)
                binding.tvTotal.text = formatCurrency(0.0)
                binding.btnPay.isEnabled = false
                binding.btnRequestShipping.isEnabled = true
                Toast.makeText(this@CheckoutActivity, "Compra existosa", Toast.LENGTH_SHORT).show()
                
                // Actualizar stock en segundo plano (o esperar si es crítico)
                for ((pid, qty) in itemsMap) {
                    val p = products.find { it.id == pid } ?: continue
                    val newStock = ((p.stock ?: 0) - qty).coerceAtLeast(0)
                    val imagesPayload = (p.images ?: emptyList()).map {
                        val path = it.path ?: (it.url ?: "")
                        val name = (it.path ?: it.url)?.substringAfterLast('/')
                        ImagePayload(access = "public", path = path, name = name, type = "image", size = it.size, mime = it.mime, meta = emptyMap())
                    }
                    val req = UpdateProductRequest(
                        name = p.name,
                        description = p.description,
                        price = p.price,
                        stock = newStock,
                        brand = p.brand,
                        category = p.category,
                        images = imagesPayload
                    )
                    try {
                        withContext(Dispatchers.IO) { productService.patchProduct(pid, req) }
                    } catch(e: Exception) {
                        // Log or ignore stock update failure if necessary
                    }
                }
                
                // Redirigir al Home correspondiente
                val dest = if (tm.isAdmin()) HomeActivity::class.java else LimitedHomeActivity::class.java
                val intent = Intent(this@CheckoutActivity, dest)
                // Limpiar back stack para evitar volver al checkout
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutActivity, "Error creando orden: ${NetworkError.message(e)}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun requestShipping() {
        // Ya no debería ser necesario si redirigimos inmediatamente después del pago, 
        // pero lo mantenemos por si acaso la lógica cambia o si falla la redirección.
        val order = currentOrder ?: return
        setLoading(true)
        lifecycleScope.launch {
            try {
                val service = RetrofitClient.createOrderService(this@CheckoutActivity)
                val updated = withContext(Dispatchers.IO) { service.updateStatus(order.id, UpdateOrderStatusRequest("enviado")) }
                currentOrder = updated
                Toast.makeText(this@CheckoutActivity, "Solicitud de envío realizada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutActivity, "Error solicitando envío: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
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

    private fun sendReceiptEmail(order: Order, pricing: Pricing) {
        val tm = TokenManager(this)
        val email = tm.getEmail()
        val subject = "Comprobante de compra #${order.id}"
        val body = buildString {
            appendLine("Gracias por su compra")
            appendLine("Orden: #${order.id}")
            appendLine("Estado: ${order.status}")
            appendLine("Subtotal: ${formatCurrency(pricing.subtotal)}")
            appendLine("Descuento: ${formatCurrency(pricing.discount)}")
            appendLine("Impuesto: ${formatCurrency(pricing.tax)}")
            appendLine("Total: ${formatCurrency(pricing.total)}")
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email ?: ""))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {}
    }
}
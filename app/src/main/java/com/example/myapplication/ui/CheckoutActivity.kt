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
import com.example.myapplication.databinding.ActivityCheckoutBinding
import com.example.myapplication.model.CreateOrderItem
import com.example.myapplication.model.CreateOrderRequest
import com.example.myapplication.model.Order
import com.example.myapplication.model.UpdateOrderStatusRequest
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
                val request = CreateOrderRequest(items, pricing.total, status = "confirmada")
                val orderService = RetrofitClient.createOrderService(this@CheckoutActivity)
                val order = withContext(Dispatchers.IO) { orderService.checkout(request) }
                currentOrder = order
                cm.clear()
                binding.tvItems.text = ""
                binding.tvSubtotal.text = formatCurrency(0.0)
                binding.tvTax.text = formatCurrency(0.0)
                binding.tvDiscount.text = formatCurrency(0.0)
                binding.tvTotal.text = formatCurrency(0.0)
                binding.btnPay.isEnabled = false
                binding.btnRequestShipping.isEnabled = true
                Toast.makeText(this@CheckoutActivity, "Orden confirmada", Toast.LENGTH_SHORT).show()
                sendReceiptEmail(order, pricing)
            } catch (e: Exception) {
                Toast.makeText(this@CheckoutActivity, "Error creando orden: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun requestShipping() {
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
        val tm = com.example.myapplication.api.TokenManager(this)
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
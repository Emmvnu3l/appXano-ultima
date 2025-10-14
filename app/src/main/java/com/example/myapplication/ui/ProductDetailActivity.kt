package com.example.myapplication.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.myapplication.databinding.ActivityProductDetailBinding
import com.example.myapplication.model.Product

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val product = intent.getSerializableExtra("product") as? Product
        if (product != null) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "$${product.price}"
            binding.tvDescription.text = product.description ?: ""
            val url = product.images?.firstOrNull()?.url
            if (url != null) binding.ivImage.load(url)
        }
    }
}
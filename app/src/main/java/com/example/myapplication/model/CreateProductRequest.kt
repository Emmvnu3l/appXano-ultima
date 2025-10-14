package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class CreateProductRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("images") val images: List<ProductImage> = emptyList()
)
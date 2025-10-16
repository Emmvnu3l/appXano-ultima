package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class CreateProductFullRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("stock") val stock: Int? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("images") val images: List<ImagePayload> = emptyList()
)


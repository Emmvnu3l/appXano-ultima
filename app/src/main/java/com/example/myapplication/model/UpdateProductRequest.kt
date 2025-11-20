package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class UpdateProductRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("stock") val stock: Int?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("category") val category: Int?
)
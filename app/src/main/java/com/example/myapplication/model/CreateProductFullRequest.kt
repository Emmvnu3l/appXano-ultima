package com.example.myapplication.model
// Request para crear producto. Incluye datos básicos y 'images' como lista de ImagePayload
// (referencias a imágenes previamente subidas vía UploadService).

import com.google.gson.annotations.SerializedName

data class CreateProductFullRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("stock") val stock: Int? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("img") val img: List<ImagePayload> = emptyList()
)


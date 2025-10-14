package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CreateProductResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("product") val product: Product? = null
) : Serializable
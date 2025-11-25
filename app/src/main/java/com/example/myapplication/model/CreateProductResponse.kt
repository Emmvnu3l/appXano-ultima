package com.example.myapplication.model
// Respuesta al crear producto: contiene el producto creado y/o metadatos de la operación.
// Se usa como retorno de ProductService.createProductFull.

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CreateProductResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("product") val product: Product? = null,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("id") val id: Int? = null
) : Serializable

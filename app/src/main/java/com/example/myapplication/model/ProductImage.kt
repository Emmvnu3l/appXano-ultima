package com.example.myapplication.model
// Representa la respuesta de subida de imagen (binario ya almacenado).
// Incluye url/path/mime/size e id si aplica.

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ProductImage(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("mime") val mime: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("path") val path: String? = null
) : Serializable
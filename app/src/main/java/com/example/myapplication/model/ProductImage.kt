package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ProductImage(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("path") val path: String? = null
) : Serializable
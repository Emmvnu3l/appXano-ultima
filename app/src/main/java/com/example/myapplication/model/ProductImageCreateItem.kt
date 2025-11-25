package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class ProductImageCreateItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("created_at") val createdAt: Any = "now",
    @SerializedName("product_id") val productId: Int,
    @SerializedName("access") val access: String = "public",
    @SerializedName("path") val path: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: String? = "image",
    @SerializedName("size") val size: Long? = null,
    @SerializedName("mime") val mime: String? = null,
    @SerializedName("meta") val meta: String? = null
)


package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Order(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("items") val items: List<OrderItem>,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("discount_code_id") val discountCodeId: Int? = null
) : Serializable

data class OrderItem(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Double?
) : Serializable
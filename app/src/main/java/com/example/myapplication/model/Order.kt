package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Order(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
    @SerializedName("total") val total: Double,
    @SerializedName("items") val items: List<OrderItem>? = emptyList(),
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("discount_code_id") val discountCodeId: Int? = null
) : Serializable

data class OrderItem(
    @SerializedName(value = "product_id", alternate = ["productId"]) val productId: Int,
    @SerializedName(value = "quantity", alternate = ["qty"]) val quantity: Int,
    @SerializedName(value = "price", alternate = ["unit_price", "pu", "price_unit"]) val price: Double?
) : Serializable

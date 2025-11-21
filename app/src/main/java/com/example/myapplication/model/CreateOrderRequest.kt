package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class CreateOrderRequest(
    @SerializedName("items") val items: List<CreateOrderItem>,
    @SerializedName("total") val total: Double,
    @SerializedName("status") val status: String = "pendiente",
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("discount_code_id") val discountCodeId: Int? = null
)

data class CreateOrderItem(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("quantity") val quantity: Int
)
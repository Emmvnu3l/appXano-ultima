package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("shipping_address") val shippingAddress: String? = null,
    @SerializedName("phone") val phone: String? = null
) : Serializable
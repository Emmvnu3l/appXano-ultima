package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("created_at") val createdAt: Long?,
    @SerializedName("blocked") val blocked: Boolean,
    @SerializedName("role") val role: String?,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("shipping_address") val shippingAddress: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("comuna") val comuna: Int?
) : Serializable

data class UserUpdateRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("blocked") val blocked: Boolean? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("shipping_address") val shippingAddress: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("comuna") val comuna: Int? = null
)

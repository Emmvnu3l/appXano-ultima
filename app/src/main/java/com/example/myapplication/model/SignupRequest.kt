package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("first_name") val first_name: String? = null,
    @SerializedName("last_name") val last_name: String? = null,
    @SerializedName("role") val role: String? = "user",
    @SerializedName("status") val status: String? = "activo",
    @SerializedName("shipping_address") val shipping_address: String? = null,
    @SerializedName("phone") val phone: String? = null
)
package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class UpdateOrderStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("admin_id") val adminId: Int? = null
)
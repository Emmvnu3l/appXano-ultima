package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class MemberEditAddressRequest(
    @SerializedName("shipping_address") val shippingAddress: String,
    @SerializedName("phone") val phone: String
)

package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CategoryCreateResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("category") val category: Category? = null
) : Serializable
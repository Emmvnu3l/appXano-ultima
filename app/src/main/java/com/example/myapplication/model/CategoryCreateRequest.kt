package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class CategoryCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("image") val image: ImagePayload
)
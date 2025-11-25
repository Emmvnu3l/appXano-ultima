package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class Region(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class Comuna(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("region_id") val regionId: Int
)

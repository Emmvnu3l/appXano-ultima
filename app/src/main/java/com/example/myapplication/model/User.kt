package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("blocked") val blocked: Boolean,
    @SerializedName("role") val role: String?
) : Serializable

data class UserUpdateRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("avatar") val avatar: String?
)
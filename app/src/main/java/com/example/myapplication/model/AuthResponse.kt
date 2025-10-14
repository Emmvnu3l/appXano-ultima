package com.example.myapplication.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AuthResponse(
    // Algunos proyectos Xano devuelven "authToken" y otros "token"; soportamos ambos.
    @SerializedName("authToken") val authToken: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("user") val user: User? = null
) : Serializable {
    fun effectiveToken(): String? = authToken ?: token
}
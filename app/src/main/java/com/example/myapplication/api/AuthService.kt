package com.example.myapplication.api

import com.example.myapplication.model.AuthResponse
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(): User
}
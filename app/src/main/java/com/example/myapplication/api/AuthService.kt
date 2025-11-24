package com.example.myapplication.api
// Servicio de autenticación: define endpoints para login, signup y obtener el usuario actual (me).
// @Body serializa el data class a JSON en el cuerpo del POST. El endpoint /auth/me requiere token.

import com.example.myapplication.model.AuthResponse
import com.example.myapplication.model.UserUpdateRequest
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.SignupRequest
import com.example.myapplication.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthService {
    // Login: envía email y password en el cuerpo. No necesita token.
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // Perfil autenticado: requiere Authorization: Bearer <token>.
    @GET("auth/me")
    suspend fun me(): User

    // Registro: crea un usuario y devuelve AuthResponse (token y user).
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    // Actualiza el perfil del usuario autenticado (parcial)
    @retrofit2.http.PATCH("auth/me")
    suspend fun updateMe(@Body request: UserUpdateRequest): User
}

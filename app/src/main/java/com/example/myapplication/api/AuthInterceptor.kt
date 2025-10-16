package com.example.myapplication.api
// Interceptor de autenticación: agrega el encabezado HTTP Authorization con esquema Bearer
// en cada petición si hay token disponible. Evita repetir el header en cada llamada.

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    // Intercepta la request y añade "Authorization: Bearer <token>" cuando corresponde.
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else original
        return chain.proceed(request)
    }
}
package com.example.myapplication.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // Este objeto centraliza la creación de clientes Retrofit para distintos servicios
    // (auth, productos, subida de imágenes). Cada cliente se construye con:
    // - Un OkHttpClient con timeouts razonables, logging y (opcionalmente) AuthInterceptor.
    // - Un Retrofit con baseUrl específica para cada grupo de endpoints y el convertidor Gson.
    // De esta forma, la autenticación (header Authorization) se aplica automáticamente
    // a todas las llamadas del cliente autenticado, sin repetir código en cada request.

    private fun okHttpClient(authenticated: Boolean, tokenManager: TokenManager?): OkHttpClient {
        // Interceptor de logging: imprime las peticiones y respuestas (útil para depurar).
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val builder = OkHttpClient.Builder()
            // Timeouts de conexión/lectura/escritura (30s) para evitar bloqueos prolongados.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
        // Si el cliente es autenticado y existe tokenManager, añadimos AuthInterceptor
        // para agregar automáticamente el header "Authorization: Bearer <token>".
        if (authenticated && tokenManager != null) {
            builder.addInterceptor(AuthInterceptor { tokenManager.getToken() })
        }
        return builder.build()
    }

    private fun retrofit(baseUrl: String, authenticated: Boolean, tokenManager: TokenManager?): Retrofit {
        // Crea una instancia de Retrofit con:
        // - baseUrl: prefijo común para los endpoints (p.ej. https://api.xano.io/...
        // - client: OkHttp configurado con logging y, si aplica, autorización.
        // - GsonConverterFactory: mapea JSON <-> data classes Kotlin.
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient(authenticated, tokenManager))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthService(context: Context): AuthService {
        // Servicio de autenticación SIN header Authorization (para login, signup).
        // Aun así, instanciamos TokenManager para reutilizar su almacenamiento si lo necesitas.
        val tm = TokenManager(context)
        return retrofit(ApiConfig.authBaseUrl, authenticated = false, tokenManager = tm)
            .create(AuthService::class.java)
    }

    // Cliente autenticado para endpoints que requieren token (p.ej. /auth/me)
    fun createAuthServiceAuthenticated(context: Context): AuthService {
        // Igual que el anterior, pero marcamos authenticated=true para que AuthInterceptor
        // agregue el token a cada request. Útil para /auth/me y cualquier endpoint protegido.
        val tm = TokenManager(context)
        return retrofit(ApiConfig.authBaseUrl, authenticated = true, tokenManager = tm)
            .create(AuthService::class.java)
    }

    fun createProductService(context: Context): ProductService {
        // Servicio de productos (requiere token). Usa baseUrl de tienda (storeBaseUrl)
        // y añade AuthInterceptor automáticamente.
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(ProductService::class.java)
    }

    fun createProductServicePublic(context: Context): ProductService {
        // Servicio de productos SIN token, para catálogos públicos.
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = false, tokenManager = tm)
            .create(ProductService::class.java)
    }

    fun createOrderService(context: Context): OrderService {
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(OrderService::class.java)
    }

    fun createUserService(context: Context): UserService {
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(UserService::class.java)
    }

    fun createUploadService(context: Context): UploadService {
        // Servicio de subida de imágenes (requiere token). Comparte la misma baseUrl
        // que ProductService y añade el token en cada petición.
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(UploadService::class.java)
    }

    fun createCategoryService(context: Context): CategoryService {
        // Servicio de categorías (requiere token). Usa la misma baseUrl de tienda.
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(CategoryService::class.java)
    }
}
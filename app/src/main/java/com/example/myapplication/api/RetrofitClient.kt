package com.example.myapplication.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private fun okHttpClient(authenticated: Boolean, tokenManager: TokenManager?): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
        if (authenticated && tokenManager != null) {
            builder.addInterceptor(AuthInterceptor { tokenManager.getToken() })
        }
        return builder.build()
    }

    private fun retrofit(baseUrl: String, authenticated: Boolean, tokenManager: TokenManager?): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient(authenticated, tokenManager))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthService(context: Context): AuthService {
        val tm = TokenManager(context)
        return retrofit(ApiConfig.authBaseUrl, authenticated = false, tokenManager = tm)
            .create(AuthService::class.java)
    }

    fun createProductService(context: Context): ProductService {
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(ProductService::class.java)
    }

    fun createUploadService(context: Context): UploadService {
        val tm = TokenManager(context)
        return retrofit(ApiConfig.storeBaseUrl, authenticated = true, tokenManager = tm)
            .create(UploadService::class.java)
    }
}
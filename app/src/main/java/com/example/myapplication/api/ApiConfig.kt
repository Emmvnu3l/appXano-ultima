package com.example.myapplication.api

import com.example.myapplication.BuildConfig

object ApiConfig {
    val authBaseUrl: String = BuildConfig.AUTH_BASE_URL
    val storeBaseUrl: String = BuildConfig.STORE_BASE_URL
    val tokenTtlSec: Int = BuildConfig.TOKEN_TTL_SEC
}
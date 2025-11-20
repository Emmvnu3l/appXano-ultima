package com.example.myapplication.api

import retrofit2.HttpException

object NetworkError {
    fun message(t: Throwable): String {
        return when (t) {
            is HttpException -> {
                try {
                    t.response()?.errorBody()?.string() ?: t.message()
                } catch (_: Exception) {
                    t.message()
                }
            }
            else -> t.message ?: "Error"
        }
    }
}
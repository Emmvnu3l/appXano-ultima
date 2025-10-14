package com.example.myapplication.api

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuth(token: String, name: String? = null, email: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getName(): String? = prefs.getString(KEY_NAME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
    }
}
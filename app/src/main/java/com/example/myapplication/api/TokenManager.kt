package com.example.myapplication.api

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveAuth(token: String, name: String? = null, email: String? = null, userId: Int? = null, role: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putInt(KEY_USER_ID, userId ?: -1)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getName(): String? = prefs.getString(KEY_NAME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getUserId(): Int? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id >= 0) id else null
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun setRole(role: String?) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun isAdmin(): Boolean {
        val role = getRole()?.lowercase()?.trim()
        return role == "admin"
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
    }
}
package com.example.myapplication.api

import com.example.myapplication.model.User
import com.example.myapplication.model.UserUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @GET("user")
    suspend fun list(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("blocked") blocked: Boolean? = null,
        @Query("q") q: String? = null,
        @Query("status") status: String? = null
    ): Response<List<User>>

    @PATCH("user/{id}")
    suspend fun update(@Path("id") id: Int, @Body request: UserUpdateRequest): User
}
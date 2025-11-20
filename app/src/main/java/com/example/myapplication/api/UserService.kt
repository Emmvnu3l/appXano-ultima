package com.example.myapplication.api

import com.example.myapplication.model.User
import com.example.myapplication.model.UserUpdateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @GET("users")
    suspend fun list(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("blocked") blocked: Boolean? = null
    ): List<User>

    @GET("users/search")
    suspend fun search(@Query("q") q: String, @Query("page") page: Int?, @Query("pageSize") pageSize: Int?): List<User>

    @PUT("users/{id}")
    suspend fun update(@Path("id") id: Int, @Body request: UserUpdateRequest): User

    @POST("users/{id}/block")
    suspend fun block(@Path("id") id: Int): User

    @POST("users/{id}/unblock")
    suspend fun unblock(@Path("id") id: Int): User
}
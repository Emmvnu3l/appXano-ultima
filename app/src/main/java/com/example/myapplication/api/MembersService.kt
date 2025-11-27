package com.example.myapplication.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.PATCH

interface MembersService {
    @Headers("Content-Type: application/json")
    @PATCH("user/edit_address")
    suspend fun editAddress(
        @Body request: com.example.myapplication.model.MemberEditAddressRequest
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @retrofit2.http.POST("user/update_status")
    suspend fun updateStatus(
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @retrofit2.http.POST("update_user_status")
    suspend fun updateUserStatus(
        @Body body: Map<String, Any>
    ): Response<ResponseBody>
}

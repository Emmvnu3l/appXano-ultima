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
}

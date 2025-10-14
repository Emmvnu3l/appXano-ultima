package com.example.myapplication.api

import com.example.myapplication.model.ProductImage
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadService {
    @Multipart
    @POST("upload")
    suspend fun uploadImage(@Part image: MultipartBody.Part): ProductImage
}
package com.example.myapplication.api
// Servicio de subida de imágenes en formato multipart. Requiere token.

import com.example.myapplication.model.ProductImage
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadService {
    // @Multipart indica que el cuerpo es multipart/form-data. @Part aporta el binario.
    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(@Part content: MultipartBody.Part): ProductImage
}
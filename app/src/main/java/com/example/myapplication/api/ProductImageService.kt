package com.example.myapplication.api

import com.example.myapplication.model.ProductImageCreateItem
import com.example.myapplication.model.ProductImage
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.POST

interface ProductImageService {
    @POST("product_image")
    suspend fun createProductImage(@Body item: ProductImageCreateItem): Unit
    
    @POST("product_images")
    suspend fun createProductImagesPlural(@Body items: List<ProductImageCreateItem>): Unit

    @GET("product_image")
    suspend fun listByProduct(@Query("product_id") productId: Int): List<ProductImage>

    @GET("product_images")
    suspend fun listByProductPlural(@Query("product_id") productId: Int): List<ProductImage>
}

package com.example.myapplication.api

import com.example.myapplication.model.CreateProductRequest
import com.example.myapplication.model.CreateProductFullRequest
import com.example.myapplication.model.CreateProductResponse
import com.example.myapplication.model.Product
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProductService {
    @GET("product")
    suspend fun getProducts(): List<Product>

    @POST("product")
    suspend fun createProduct(@Body request: CreateProductRequest): CreateProductResponse

    @POST("product")
    suspend fun createProductFull(@Body request: CreateProductFullRequest): CreateProductResponse
}
package com.example.myapplication.api
// Servicio de productos: listar y crear productos. Requiere cliente autenticado (token).

import com.example.myapplication.model.CreateProductFullRequest
import com.example.myapplication.model.CreateProductBasicRequest
import com.example.myapplication.model.CreateProductResponse
import com.example.myapplication.model.Product
import com.example.myapplication.model.UpdateProductRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.http.Path

interface ProductService {
    // Lista de productos (GET /product).
    @GET("product")
    suspend fun getProducts(): List<Product>

    // Crear producto completo: @Body => JSON con datos e imágenes (ImagePayload).
    @POST("product")
    suspend fun createProductFull(@Body request: CreateProductFullRequest): CreateProductResponse

    // Crear producto básico (sin imágenes) según API v0.0.1
    @POST("product")
    suspend fun createProductBasic(@Body request: CreateProductBasicRequest): CreateProductResponse

    @PUT("product/{id}")
    suspend fun updateProduct(@Path("id") id: Int, @Body request: UpdateProductRequest): Product

    @PATCH("product/{id}")
    suspend fun patchProduct(@Path("id") id: Int, @Body request: UpdateProductRequest): Product

    @GET("product/{id}")
    suspend fun getProduct(@Path("id") id: Int): Product

    @DELETE("product/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Unit
}

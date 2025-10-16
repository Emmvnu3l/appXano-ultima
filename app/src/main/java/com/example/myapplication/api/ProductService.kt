package com.example.myapplication.api
// Servicio de productos: listar y crear productos. Requiere cliente autenticado (token).

import com.example.myapplication.model.CreateProductFullRequest
import com.example.myapplication.model.CreateProductResponse
import com.example.myapplication.model.Product
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProductService {
    // Lista de productos (GET /product).
    @GET("product")
    suspend fun getProducts(): List<Product>

    // Crear producto completo: @Body => JSON con datos e imágenes (ImagePayload).
    @POST("product")
    suspend fun createProductFull(@Body request: CreateProductFullRequest): CreateProductResponse
}
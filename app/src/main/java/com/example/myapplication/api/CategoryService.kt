package com.example.myapplication.api

import com.example.myapplication.model.Category
import com.example.myapplication.model.CategoryCreateRequest
import com.example.myapplication.model.CategoryCreateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CategoryService {
    // Crear categoría: POST /category con { name, description, image }
    @POST("category")
    suspend fun createCategory(@Body request: CategoryCreateRequest): CategoryCreateResponse

    // Listar categorías: GET /category
    @GET("category")
    suspend fun getCategories(): List<Category>
}
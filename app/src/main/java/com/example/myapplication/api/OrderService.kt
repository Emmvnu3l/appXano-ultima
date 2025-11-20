package com.example.myapplication.api

import com.example.myapplication.model.CreateOrderRequest
import com.example.myapplication.model.Order
import com.example.myapplication.model.UpdateOrderStatusRequest
import retrofit2.http.Query
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

interface OrderService {
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Order

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): Order

    @GET("orders")
    suspend fun listOrders(
        @Query("status") status: String?,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?
    ): List<Order>

    @PUT("orders/{id}/accept")
    suspend fun accept(@Path("id") id: Int): Order

    @PUT("orders/{id}/reject")
    suspend fun reject(@Path("id") id: Int): Order

    @PUT("orders/{id}/ship")
    suspend fun ship(@Path("id") id: Int): Order

    @PATCH("orders/{id}")
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body request: UpdateOrderStatusRequest
    ): Order
}
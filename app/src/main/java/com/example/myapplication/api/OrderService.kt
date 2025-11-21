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
    @POST("order")
    suspend fun createOrder(@Body request: CreateOrderRequest): Order

    @POST("order")
    suspend fun createOrderRaw(@Body body: Map<String, @JvmSuppressWildcards Any>): Order

    @POST("order/checkout")
    suspend fun checkout(@Body request: CreateOrderRequest): Order

    @GET("order/{id}")
    suspend fun getOrder(@Path("id") id: Int): Order

    @GET("order")
    suspend fun listOrders(
        @Query("status") status: String?,
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null,
        @Query("type") type: String? = null
    ): List<Order>

    @PUT("order/{id}/accept")
    suspend fun accept(@Path("id") id: Int): Order

    @PUT("order/{id}/reject")
    suspend fun reject(@Path("id") id: Int): Order

    @PUT("order/{id}/ship")
    suspend fun ship(@Path("id") id: Int): Order

    @PATCH("order/{id}")
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body request: UpdateOrderStatusRequest
    ): Order
}
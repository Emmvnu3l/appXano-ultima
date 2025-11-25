package com.example.myapplication.api

import com.example.myapplication.model.Region
import com.example.myapplication.model.Comuna
import retrofit2.http.GET
import retrofit2.http.Path

interface GeoService {
    @GET("region")
    suspend fun getRegions(): List<Region>

    @GET("region/{id}/comuna")
    suspend fun getComunas(@Path("id") regionId: Int): List<Comuna>

    @GET("comuna/{id}")
    suspend fun getComuna(@Path("id") id: Int): Comuna
}

package com.softinsa.sf2019_tracker

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


public interface apiService {

    @GET("routes")
    fun getRoutes(): Call<MutableList<Route>>

    @GET("processions")
    fun getProcessions(): Call<MutableList<MutableList<Procession>>>

    @GET("get_procession_tracker")
    fun getProcessionsTracker(@Query("id") id: String): Call<MutableList<LocationSpecReturn>>

    @POST("post_procession_tracker")
    fun postLocation(@Body user: LocationSpec): Call<String>

    @POST("update_procession_route")
    fun updateRoute(@Body user: RouteId): Call<String>

    @POST("procession")
    fun postProcession(@Body user: LocationSpec): Call<String>

    @POST("processionFinish")
    fun postProcessionFinish(@Body user: LocationSpec): Call<String>

    @POST("post_location")
    fun postLocationWithID(@Body loc: LocationWithIDSpec): Call<String>

}


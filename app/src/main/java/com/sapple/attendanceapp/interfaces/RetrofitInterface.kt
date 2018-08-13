package com.sapple.attendanceapp.interfaces

import com.sapple.attendanceapp.dataclasses.Login
import io.reactivex.Observable
import retrofit2.http.*
import com.google.gson.JsonObject

interface RetrofitInterface  {

    //@FormUrlEncoded
    @POST("customers")
    @Headers("Content-Type: application/json")
    fun getData(@Body jsonBody: JsonObject ) : Observable<Login>

    @POST("terminal_images")
    @Headers("Content-Type: application/json")
    fun submitData(@Body jsonBody: JsonObject ): Observable<Login>
}
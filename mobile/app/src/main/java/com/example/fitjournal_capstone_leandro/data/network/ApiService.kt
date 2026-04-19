package com.example.fitjournal_capstone_leandro.data.network

import com.example.fitjournal_capstone_leandro.data.model.Exercise
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Headers


interface ApiService {

    @Headers(
        "X-RapidAPI-Key: 3f94bc08abmsh94a3be838b0bdd0p12eea4jsn03d3a9f0910b",
        "X-RapidAPI-Host: exercisedb.p.rapidapi.com"
    )
    @GET("exercises/targetList")
    suspend fun getMuscleList(): List<String>

    @Headers(
        "X-RapidAPI-Key: 3f94bc08abmsh94a3be838b0bdd0p12eea4jsn03d3a9f0910b",
        "X-RapidAPI-Host: exercisedb.p.rapidapi.com"
    )
    @GET("exercises/target/{muscle}")
    suspend fun getExercisesByMuscle(
        @Path("muscle") muscle: String
    ): List<Exercise>
}

val service: ApiService = Retrofit.Builder()
    .baseUrl("https://exercisedb.p.rapidapi.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    .create(ApiService::class.java)
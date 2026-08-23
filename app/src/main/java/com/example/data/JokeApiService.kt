package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface JokeService {
    @GET("random")
    suspend fun getRandomJoke(
        @Query("format") format: String = "json"
    ): JokeResponse
}

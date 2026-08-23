package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JokeResponse(
    @Json(name = "id")
    val id: Int? = null,
    @Json(name = "type")
    val type: String? = null,
    @Json(name = "setup")
    val setup: String? = null,
    @Json(name = "delivery")
    val delivery: String? = null,
    @Json(name = "joke")
    val joke: String? = null,
    @Json(name = "error")
    val error: Boolean? = null
)

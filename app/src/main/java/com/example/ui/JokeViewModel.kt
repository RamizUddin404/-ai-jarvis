package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JokeResponse
import com.example.data.JokeService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class JokeUiState(
    val joke: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val jokesHistory: List<String> = emptyList()
)

class JokeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(JokeUiState())
    val uiState: StateFlow<JokeUiState> = _uiState.asStateFlow()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://v2.jokeapi.dev/joke/")
        .addConverterFactory(
            MoshiConverterFactory.create(
                Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
            )
        )
        .build()

    private val jokeService = retrofit.create(JokeService::class.java)

    fun getRandomJoke() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = jokeService.getRandomJoke()
                val jokeText = when {
                    response.error == true -> "Failed to load joke"
                    response.type == "single" -> response.joke ?: "No joke available"
                    response.type == "twopart" -> "${response.setup}\n\n${response.delivery}"
                    else -> "No joke available"
                }

                val newHistory = (listOf(jokeText) + _uiState.value.jokesHistory).take(10)
                _uiState.value = _uiState.value.copy(
                    joke = jokeText,
                    isLoading = false,
                    jokesHistory = newHistory
                )
            } catch (e: Exception) {
                Log.e("JokeViewModel", "Error fetching joke", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(jokesHistory = emptyList())
    }
}

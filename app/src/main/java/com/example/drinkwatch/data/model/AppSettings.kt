package com.example.drinkwatch.data.model

enum class Theme { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: Theme = Theme.SYSTEM,
    val activeDrinkHighlight: Int = 3,
    val defaultTimeoutSeconds: Int = 300,
)

package com.example.drinkwatch.data.model

enum class DrinkType { SHOT, LONG_DRINK, NON_ALCOHOLIC }

data class Drink(
    val id: Long,
    val sessionId: Long,
    val name: String,
    val type: DrinkType,
    val isDisabled: Boolean,
)

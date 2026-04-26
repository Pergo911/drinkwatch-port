package com.example.drinkwatch.data.model

data class Player(
    val id: Long,
    val sessionId: Long,
    val name: String,
    val phone: String,
    val note: String,
    val isDisabled: Boolean,
)

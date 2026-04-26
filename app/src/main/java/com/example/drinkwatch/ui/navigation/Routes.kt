package com.example.drinkwatch.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface AppRoute : NavKey

@Serializable data object Main         : AppRoute
@Serializable data object Session      : AppRoute
@Serializable data object Settings     : AppRoute
@Serializable data object About        : AppRoute
@Serializable data class  PlayerDetail(val playerId: Long) : AppRoute
@Serializable data class  OrderDialog(val playerId: Long)  : AppRoute

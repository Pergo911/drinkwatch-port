package com.example.drinkwatch.viewmodel

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class Res(@StringRes val id: Int) : UiText

    fun asString(context: Context): String = when (this) {
        is Res -> context.getString(id)
    }
}

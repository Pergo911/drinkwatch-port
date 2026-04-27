package com.example.drinkwatch.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/**
 * Traverses the context chain to find the nearest [ComponentActivity].
 * Required when composables are hosted inside a dialog, where [Context] is a
 * [ContextWrapper] rather than the Activity itself.
 */
fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error("No ComponentActivity found in context chain: $this")
}

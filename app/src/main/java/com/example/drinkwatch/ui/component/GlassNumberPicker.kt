package com.example.drinkwatch.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private const val MaxGlassNumber = 99

@Composable
fun GlassNumberPicker(
    selectedNumber: Int,
    takenNumbers: Set<Int>,
    onNumberChange: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier.alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Glass #",
                style = MaterialTheme.typography.labelMedium,
            )
            WheelPicker(
                items = (1..MaxGlassNumber).map { it.toString() },
                initialIndex = (selectedNumber - 1).coerceIn(0, MaxGlassNumber - 1),
                onIndexChange = { onNumberChange(it + 1) },
                modifier = Modifier.width(WheelWidth),
                itemContent = { label, isCenter ->
                    val number = label.toIntOrNull()
                    val isTaken = number != null && number in takenNumbers
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTaken) errorColor else Color.Unspecified,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        }
        if (!enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }
    }
}

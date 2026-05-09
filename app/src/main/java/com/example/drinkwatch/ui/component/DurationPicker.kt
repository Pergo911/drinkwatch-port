package com.example.drinkwatch.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.drinkwatch.R
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

internal val ItemHeight = 48.dp
internal val WheelWidth = 80.dp
private const val MaxHours = 99

@Composable
fun DurationPicker(
    initialSeconds: Int,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hours by remember { mutableIntStateOf((initialSeconds / 3600).coerceIn(0, MaxHours)) }
    var minutes by remember { mutableIntStateOf((initialSeconds % 3600) / 60) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.duration_picker_hours),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            WheelPicker(
                items = (0..MaxHours).map { it.toString().padStart(2, '0') },
                initialIndex = hours,
                onIndexChange = { newHours ->
                    hours = newHours
                    onSecondsChange(newHours * 3600 + minutes * 60)
                },
                modifier = Modifier.width(WheelWidth),
            )
        }

        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.duration_picker_minutes),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            WheelPicker(
                items = (0..59).map { it.toString().padStart(2, '0') },
                initialIndex = minutes,
                onIndexChange = { newMinutes ->
                    minutes = newMinutes
                    onSecondsChange(hours * 3600 + newMinutes * 60)
                },
                modifier = Modifier.width(WheelWidth),
            )
        }
    }
    }
}

@Composable
internal fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (label: String, isCenter: Boolean) -> Unit = { label, isCenter ->
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    },
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceIn(0, items.lastIndex),
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val halfItemHeightPx = with(LocalDensity.current) { ItemHeight.toPx() / 2f }
    // Pad with one empty slot on each end so items[0] and items[last]
    // can scroll to the center position. firstVisibleItemIndex then maps
    // directly to items[firstVisibleItemIndex].
    val paddedItems = remember(items) { listOf("") + items + listOf("") }
    val view = LocalView.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .drop(1) // skip initial false emission at first composition
            .collect { onIndexChange(listState.firstVisibleItemIndex) }
    }

    // Fire a haptic tick each time a new item scrolls into the center position.
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemScrollOffset >= halfItemHeightPx) {
                listState.firstVisibleItemIndex + 2
            } else {
                listState.firstVisibleItemIndex + 1
            }
        }
            .drop(1) // skip emission on first composition
            .collect { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }

    Box(modifier = modifier.height(ItemHeight * 3)) {
        // Selection highlight behind the center row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(ItemHeight)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(paddedItems) { paddedIndex, label ->
                // derivedStateOf per-item so only the two items whose center-status
                // actually changes (prev center → not, new center → yes) recompose
                // when firstVisibleItemIndex updates.
                val isCenter by remember {
                    derivedStateOf {
                        val centerIndex =
                            if (listState.firstVisibleItemScrollOffset >= halfItemHeightPx) {
                                listState.firstVisibleItemIndex + 2
                            } else {
                                listState.firstVisibleItemIndex + 1
                            }
                        paddedIndex == centerIndex
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .alpha(if (isCenter) 1f else 0.35f),
                ) {
                    itemContent(label, isCenter)
                }
            }
        }
    }
}

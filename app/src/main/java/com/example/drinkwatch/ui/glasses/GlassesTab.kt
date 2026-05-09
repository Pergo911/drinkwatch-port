package com.example.drinkwatch.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.drinkwatch.R
import com.example.drinkwatch.ui.dialog.ReturnGlassDialog
import com.example.drinkwatch.ui.theme.Dimens
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun GlassesTab(
    takenGlasses: List<TakenGlassUiState>,
    onReturnGlass: (Char, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var returnTarget by remember { mutableStateOf<TakenGlassUiState?>(null) }

    if (takenGlasses.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.LocalBar,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.empty_all_glasses_returned),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.ScreenVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = takenGlasses,
                key = { "${it.glassGroup}${it.glassNumber}" },
            ) { uiState ->
                GlassCard(
                    uiState = uiState,
                    onClick = { returnTarget = uiState },
                )
            }
        }
    }

    returnTarget?.let { target ->
        ReturnGlassDialog(
            glass = target,
            onConfirm = {
                onReturnGlass(target.glassGroup, target.glassNumber)
                returnTarget = null
            },
            onDismiss = { returnTarget = null },
        )
    }
}

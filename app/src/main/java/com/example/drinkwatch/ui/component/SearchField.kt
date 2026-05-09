package com.example.drinkwatch.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.drinkwatch.R
import com.example.drinkwatch.ui.theme.Dimens

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    collapsedHorizontalPadding: Dp = Dimens.ScreenHorizontalPadding,
    collapsedVerticalPadding: Dp = Dimens.ScreenVerticalPadding,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    BackHandler(enabled = expanded, onBack = onCollapse)

    val transition = updateTransition(targetState = expanded, label = "searchExpand")

    val horizontalPad by transition.animateDp(label = "hPad") {
        if (it) 0.dp else collapsedHorizontalPadding
    }
    val verticalPad by transition.animateDp(label = "vPad") {
        if (it) 0.dp else collapsedVerticalPadding
    }
    val cornerRadius by transition.animateDp(label = "corner") {
        if (it) 0.dp else 50.dp
    }
    val containerColor by transition.animateColor(label = "containerColor") {
        if (it) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus()
    }

    Column(modifier = modifier.padding(horizontal = horizontalPad, vertical = verticalPad)) {
        Box {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    Crossfade(targetState = expanded, label = "leadingIcon") { isExpanded ->
                        if (isExpanded) {
                            IconButton(onClick = onCollapse) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_close_search),
                                )
                            }
                        } else {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                trailingIcon = trailingIcon,
                singleLine = true,
                enabled = expanded,
                shape = RoundedCornerShape(cornerRadius),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledContainerColor = containerColor,
                    disabledIndicatorColor = Color.Transparent,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            // Invisible overlay: captures taps when collapsed to open search mode.
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onExpand,
                        )
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            HorizontalDivider()
        }
    }
}

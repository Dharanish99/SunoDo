package com.sunodo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// SunoDo is intentionally a single dark theme by design (see the demo's
// "quiet during meetings/commutes" framing in docs/blueprint.md) rather than
// a light/dark adaptive one — isSystemInDarkTheme() is unused for now but
// left as the obvious extension point if that decision changes later.
private val SunoDoColorScheme = darkColorScheme(
    primary = Marigold,
    onPrimary = Ink,
    secondary = Sage,
    tertiary = Coral,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    outline = Hairline
)

@Composable
fun SunoDoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SunoDoColorScheme,
        typography = SunoDoTypography,
        content = content
    )
}

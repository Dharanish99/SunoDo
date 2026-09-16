package com.sunodo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunodo.app.ui.theme.Coral

/**
 * Shown on both ProcessingScreen and ActionCardScreen whenever
 * PacketExtractorFactory fell back to the Stage 2 stub because no on-device
 * model file was found (see ModelPaths.kt / android/README.md). Without
 * this, a real device-tier badge ("High-tier device · Gemma-3n E2B") sits
 * next to a canned example with nothing telling you the two are unrelated —
 * which looks like a working demo instead of a fallback.
 */
@Composable
fun ModelStatusBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Coral.copy(alpha = 0.14f))
            .border(1.dp, Coral, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Example output — no on-device model file found on this device. " +
                "This is the Stage 2 stub, not a real extraction. See android/README.md \u2192 ModelPaths.kt.",
            style = MaterialTheme.typography.bodySmall,
            color = Coral,
            textAlign = TextAlign.Start
        )
    }
}

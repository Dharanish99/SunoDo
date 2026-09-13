package com.sunodo.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sunodo.app.pipeline.DeviceTier
import com.sunodo.app.ui.theme.Hairline
import com.sunodo.app.ui.theme.Marigold
import com.sunodo.app.ui.theme.Sage
import com.sunodo.app.ui.theme.Steel
import com.sunodo.app.ui.theme.TextMuted

/**
 * The tier badge is not cosmetic filler — it's meant to make the adaptive
 * branch in docs/blueprint.md §3.4 visible. Stage 3 passes a real DeviceTier
 * here once the actual capability check + model are wired in; until then
 * this shows no badge while the stub is running (deviceTier == null).
 */
@Composable
fun ProcessingScreen(deviceTier: DeviceTier?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        deviceTier?.let { tier ->
            val (dotColor, label) = when (tier) {
                DeviceTier.HIGH -> Sage to "High-tier device \u00b7 Gemma-3n E2B (audio-native)"
                DeviceTier.BUDGET -> Steel to "Budget-tier device \u00b7 ML Kit + Gemma 3 1B"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(dotColor)
                    )
                    Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        CircularProgressIndicator(color = Marigold, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Extracting packets\u2026",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

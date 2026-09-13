package com.sunodo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunodo.app.ui.theme.Ink
import com.sunodo.app.ui.theme.Marigold
import com.sunodo.app.ui.theme.TextMuted

/** Sample transcript kept identical to the "Ramesh (Manager)" scenario in
 * demo/sunodo_demo.html, so the Android app and the web demo tell the same
 * story rather than two different ones. */
const val SAMPLE_TRANSCRIPT = "Hey listen, uh, so I just got off the call with finance and " +
    "honestly it went a bit sideways, they're saying the numbers Priya sent don't reconcile " +
    "with what marketing submitted last week, so that's a whole thing we'll have to sort out " +
    "separately. But main reason I'm calling — I need the Q3 report on my desk by Tuesday " +
    "morning at the latest, our director is presenting it Wednesday and I can't be scrambling " +
    "again like last quarter. Also, quick one, do you know if the client ever confirmed the " +
    "revised budget? Nobody's mentioned it in like two weeks and I don't want to walk into " +
    "Wednesday without an answer. Oh, and one more thing, we moved Thursday's sync from 11 " +
    "to 4pm, the conference room got double-booked. Alright, that's it, let me know on the report."

@Composable
fun HomeScreen(receivedLabel: String?, onTrySample: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Listen once.\nAct instantly.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = receivedLabel
                ?: "Share a voice note here from WhatsApp (or any app) to see it turn into action cards.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onTrySample,
            colors = ButtonDefaults.buttonColors(containerColor = Marigold, contentColor = Ink),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try a sample voice note")
        }
    }
}

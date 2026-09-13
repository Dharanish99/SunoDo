package com.sunodo.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunodo.app.data.Packet
import com.sunodo.app.viewmodel.UiState
import com.sunodo.app.viewmodel.VoiceNoteViewModel

/**
 * The single composable both MainActivity and ShareReceiverActivity host.
 * It doesn't know or care who its caller is — only what UiState says.
 * `onAction` is intentionally a no-op placeholder toast until Stage 4 wires
 * OSActionBridge in; everything else here is real, not staged.
 */
@Composable
fun SunoDoScreen(
    receivedLabel: String? = null,
    autoStartTranscript: String? = null,
    onPacketAction: (Packet) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: VoiceNoteViewModel = viewModel(factory = VoiceNoteViewModel.factory(context))
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(autoStartTranscript) {
        if (autoStartTranscript != null && uiState is UiState.Idle) {
            viewModel.processVoiceNote(autoStartTranscript, sourceApp = "WhatsApp")
        }
    }

    when (val state = uiState) {
        is UiState.Idle -> HomeScreen(
            receivedLabel = receivedLabel,
            onTrySample = { viewModel.processVoiceNote(SAMPLE_TRANSCRIPT) }
        )
        is UiState.Processing -> ProcessingScreen(deviceTier = state.deviceTier)
        is UiState.Result -> ActionCardScreen(
            tldr = state.tldr,
            packets = state.packets,
            onAction = onPacketAction,
            onDismiss = { packet -> viewModel.dismiss(packet.id) },
            onReset = { viewModel.reset() }
        )
        is UiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = { viewModel.reset() }
        )
    }
}

package com.sunodo.app.viewmodel

import com.sunodo.app.data.Packet
import com.sunodo.app.pipeline.DeviceTier

sealed interface UiState {
    data object Idle : UiState
    data class Processing(val deviceTier: DeviceTier?, val usingRealModel: Boolean) : UiState
    data class Result(
        val voiceNoteId: Long,
        val tldr: String,
        val packets: List<Packet>,
        val usingRealModel: Boolean
    ) : UiState
    data class Error(val message: String) : UiState
}

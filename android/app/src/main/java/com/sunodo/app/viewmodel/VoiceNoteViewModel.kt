package com.sunodo.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sunodo.app.data.Packet
import com.sunodo.app.data.PacketDao
import com.sunodo.app.data.SunoDoDatabase
import com.sunodo.app.data.VoiceNote
import com.sunodo.app.pipeline.PacketExtractor
import com.sunodo.app.pipeline.StubPacketExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Orchestrates one voice note through the pipeline and owns the screen state.
 * The extraction step is swappable (see PacketExtractor) — everything either
 * side of it, including the two real Room writes below, is not a stub.
 */
class VoiceNoteViewModel(
    private val packetDao: PacketDao,
    private val extractor: PacketExtractor = StubPacketExtractor()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun processVoiceNote(transcript: String, sourceApp: String = "WhatsApp", durationSec: Int = 0) {
        viewModelScope.launch {
            _uiState.value = UiState.Processing(deviceTier = null)
            try {
                val result = extractor.extract(transcript, sourceApp, durationSec)

                val voiceNote = VoiceNote(
                    sourceApp = sourceApp,
                    durationSec = durationSec,
                    tldr = result.tldr,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    deviceTier = result.deviceTier.name
                )
                val packetEntities = result.packets.map {
                    Packet(
                        voiceNoteId = 0, // overwritten by insertVoiceNoteWithPackets
                        type = it.type,
                        content = it.content,
                        dueEpochDay = it.dueEpochDay,
                        replySuggestion = it.replySuggestion
                    )
                }

                // Real round-trip through Room: write, then read back what was
                // actually persisted (so ids in the UI are real row ids, not
                // guesses), not just holding the in-memory result in state.
                val voiceNoteId = packetDao.insertVoiceNoteWithPackets(voiceNote, packetEntities)
                val savedPackets = packetDao.observePackets(voiceNoteId).first()

                _uiState.value = UiState.Result(
                    voiceNoteId = voiceNoteId,
                    tldr = result.tldr,
                    packets = savedPackets
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "Something went wrong while reading that voice note."
                )
            }
        }
    }

    fun dismiss(packetId: Long) {
        viewModelScope.launch {
            packetDao.dismissPacket(packetId)
            val current = _uiState.value
            if (current is UiState.Result) {
                _uiState.value = current.copy(packets = current.packets.filterNot { it.id == packetId })
            }
        }
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer<VoiceNoteViewModel> {
                VoiceNoteViewModel(SunoDoDatabase.getInstance(context.applicationContext).packetDao())
            }
        }
    }
}

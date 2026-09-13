package com.sunodo.app.pipeline

import com.sunodo.app.data.PacketType

/**
 * The seam the two real tiers (and Stage 2's stub) all implement.
 * Changed in Stage 3 from a plain `transcript: String` parameter to
 * `VoiceInput`: Stage 2 assumed every path receives already-transcribed
 * text, but the high-tier path is audio-native by design and never produces
 * a separate transcript at all. Everything above this interface — Room,
 * the ViewModel, the Compose UI — is unaffected by that change.
 */
interface PacketExtractor {
    suspend fun extract(input: VoiceInput, sourceApp: String, durationSec: Int): ExtractionResult
}

data class ExtractionResult(
    val tldr: String,
    val packets: List<ExtractedPacket>,
    val deviceTier: DeviceTier
)

data class ExtractedPacket(
    val type: PacketType,
    val content: String,
    val dueEpochDay: Long?,
    val replySuggestion: String?
)

enum class DeviceTier { HIGH, BUDGET }

package com.sunodo.app.pipeline

import com.sunodo.app.data.PacketType

/**
 * The seam Stage 3 plugs into. Everything above this interface — the Share
 * target, Room persistence, the Compose UI, the ViewModel — does not know or
 * care whether the implementation underneath is a stub, ML Kit + Gemma 3 1B,
 * or Gemma-3n reading raw audio directly. That's the point of the device-tier
 * branch in docs/blueprint.md §3.4: two very different extraction paths,
 * one interface.
 */
interface PacketExtractor {
    suspend fun extract(transcript: String, sourceApp: String, durationSec: Int): ExtractionResult
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

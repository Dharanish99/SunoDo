package com.sunodo.app.pipeline

import com.sunodo.app.data.PacketType
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Kept from Stage 2, updated to the VoiceInput-based interface. Also used
 * by PacketExtractorFactory as a graceful fallback when a tier's model file
 * isn't present on the device yet — `tier` lets that fallback still show
 * the real detected tier in the UI rather than a hardcoded one.
 */
class StubPacketExtractor(private val tier: DeviceTier = DeviceTier.HIGH) : PacketExtractor {
    override suspend fun extract(input: VoiceInput, sourceApp: String, durationSec: Int): ExtractionResult {
        delay(1400)
        val today = LocalDate.now().toEpochDay()
        return ExtractionResult(
            tldr = "Report due Tuesday, client's budget still unconfirmed, Thursday sync moved to 4pm.",
            packets = listOf(
                ExtractedPacket(PacketType.TASK, "Send the Q3 report", today + 3, null),
                ExtractedPacket(
                    PacketType.QUESTION,
                    "Confirm the client's revised budget",
                    null,
                    "Checking on the client's revised budget now, will confirm shortly."
                ),
                ExtractedPacket(PacketType.DECISION, "Thursday sync moved from 11am to 4pm", today + 5, null),
                ExtractedPacket(PacketType.INFO, "New vendor contact shared: Ramesh, Chennai", null, null)
            ),
            deviceTier = tier
        )
    }
}

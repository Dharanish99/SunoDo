package com.sunodo.app.pipeline

import com.sunodo.app.data.PacketType
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Stage 2's placeholder for the real model call. It ignores the actual
 * transcript and returns the same worked example from docs/blueprint.md §3.2,
 * with dates resolved relative to today — enough to exercise the Room
 * round-trip and the full Compose UI without a model in the loop yet.
 *
 * Stage 3 replaces this with two real implementations (one per device tier)
 * behind the same PacketExtractor interface; nothing above this file changes.
 */
class StubPacketExtractor : PacketExtractor {
    override suspend fun extract(transcript: String, sourceApp: String, durationSec: Int): ExtractionResult {
        delay(1400) // stands in for on-device inference latency
        val today = LocalDate.now().toEpochDay()
        return ExtractionResult(
            tldr = "Report due Tuesday, client's budget still unconfirmed, Thursday sync moved to 4pm.",
            packets = listOf(
                ExtractedPacket(
                    type = PacketType.TASK,
                    content = "Send the Q3 report",
                    dueEpochDay = today + 3,
                    replySuggestion = null
                ),
                ExtractedPacket(
                    type = PacketType.QUESTION,
                    content = "Confirm the client's revised budget",
                    dueEpochDay = null,
                    replySuggestion = "Checking on the client's revised budget now, will confirm shortly."
                ),
                ExtractedPacket(
                    type = PacketType.DECISION,
                    content = "Thursday sync moved from 11am to 4pm",
                    dueEpochDay = today + 5,
                    replySuggestion = null
                ),
                ExtractedPacket(
                    type = PacketType.INFO,
                    content = "New vendor contact shared: Ramesh, Chennai",
                    dueEpochDay = null,
                    replySuggestion = null
                )
            ),
            deviceTier = DeviceTier.HIGH
        )
    }
}

package com.sunodo.app.pipeline

import android.content.Context

/**
 * Picks the tier, then degrades gracefully to the stub if that tier's model
 * file isn't actually on the device — model files are too large to commit
 * to this repo (see ModelPaths), so "not present" is the expected state
 * until one is pushed by hand for testing.
 *
 * `usingRealModel` exists because the device-tier badge (a real capability
 * check) and the extraction result (possibly the stub) used to be shown
 * together with no indication that one was real and the other wasn't —
 * genuinely confusing, since a "High-tier device · Gemma-3n E2B" badge next
 * to a canned example looks like a working demo instead of a fallback.
 */
data class ExtractorSelection(val extractor: PacketExtractor, val usingRealModel: Boolean)

object PacketExtractorFactory {
    fun create(context: Context): ExtractorSelection {
        val tier = DeviceTierDetector.detect(context)
        val modelReady = when (tier) {
            DeviceTier.HIGH -> ModelPaths.isHighTierModelPresent(context)
            DeviceTier.BUDGET -> ModelPaths.isBudgetTierModelPresent(context)
        }
        if (!modelReady) {
            return ExtractorSelection(StubPacketExtractor(tier = tier), usingRealModel = false)
        }
        val extractor: PacketExtractor = when (tier) {
            DeviceTier.HIGH -> HighTierPacketExtractor(context)
            DeviceTier.BUDGET -> BudgetTierPacketExtractor(context)
        }
        return ExtractorSelection(extractor, usingRealModel = true)
    }
}

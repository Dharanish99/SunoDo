package com.sunodo.app.pipeline

import android.content.Context

/**
 * Picks the tier, then degrades gracefully to the stub if that tier's model
 * file isn't actually on the device — model files are too large to commit
 * to this repo (see ModelPaths), so "not present" is the expected state
 * until one is pushed by hand for testing.
 */
object PacketExtractorFactory {
    fun create(context: Context): PacketExtractor {
        val tier = DeviceTierDetector.detect(context)
        val modelReady = when (tier) {
            DeviceTier.HIGH -> ModelPaths.isHighTierModelPresent(context)
            DeviceTier.BUDGET -> ModelPaths.isBudgetTierModelPresent(context)
        }
        if (!modelReady) return StubPacketExtractor(tier = tier)
        return when (tier) {
            DeviceTier.HIGH -> HighTierPacketExtractor(context)
            DeviceTier.BUDGET -> BudgetTierPacketExtractor(context)
        }
    }
}

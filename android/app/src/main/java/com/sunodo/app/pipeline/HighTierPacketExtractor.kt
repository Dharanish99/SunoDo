package com.sunodo.app.pipeline

import android.content.Context

/** docs/blueprint.md high-tier path: Gemma-3n E2B, audio-native, single pass. */
class HighTierPacketExtractor(context: Context) :
    PacketExtractor by LlmPacketExtractor(
        context = context,
        modelPath = ModelPaths.highTierModel(context).absolutePath,
        tier = DeviceTier.HIGH,
        maxTokens = 1024
    )

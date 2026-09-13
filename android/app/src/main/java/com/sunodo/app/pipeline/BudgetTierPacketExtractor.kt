package com.sunodo.app.pipeline

import android.content.Context

/**
 * docs/blueprint.md's budget-tier path was originally spec'd as ML Kit
 * on-device speech recognition producing a transcript, then Gemma 3 1B
 * structuring it. While building this stage: as far as could be confirmed,
 * neither ML Kit nor Android's platform SpeechRecognizer documents a public
 * API for transcribing an already-recorded audio FILE — both are built
 * around live microphone dictation (SpeechRecognizer drives its own
 * AudioRecord session; there's no recognizeFile(uri) entry point). That's a
 * real mismatch with what this app actually needs: the audio already exists
 * as a file by the time it reaches SunoDo, nothing is spoken live into it.
 *
 * This is exactly the kind of assumption docs/blueprint.md §3.4 modeled
 * validating rather than building past (there, the ~5.9GB Gemma-3n RAM
 * footprint). So: this class points the same audio-native MediaPipe
 * mechanism used by the high tier at a smaller model checkpoint instead of
 * a separate transcribe-then-structure pipeline. Two concrete ways to
 * validate this further are in android/README.md's "Open questions" section.
 */
class BudgetTierPacketExtractor(context: Context) :
    PacketExtractor by LlmPacketExtractor(
        context = context,
        modelPath = ModelPaths.budgetTierModel(context).absolutePath,
        tier = DeviceTier.BUDGET,
        maxTokens = 512
    )

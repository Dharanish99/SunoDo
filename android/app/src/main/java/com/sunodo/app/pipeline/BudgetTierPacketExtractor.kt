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
 * a separate transcribe-then-structure pipeline.
 *
 * A follow-up fix confirmed the high-tier audio call itself (see
 * LlmPacketExtractor.kt) but surfaced a second, more fundamental problem
 * for THIS class specifically: live research turned up audio-capable
 * checkpoints for Gemma-3n and the newer Gemma 4, but nothing indicating
 * Gemma 3 1B has an audio-capable checkpoint at all — it appears to be
 * text-only, full stop, not just "the checkpoint I picked lacks it." If
 * that holds up, this class's audio path (VoiceInput.Audio) will fail at
 * runtime no matter how correct the addAudio() call is, and the
 * transcribe-then-structure split from the original design may need to
 * come back for this tier specifically, with a real answer for the file-
 * based-STT gap above. Text input (VoiceInput.Transcript) is unaffected —
 * that path never touches audio modality at all.
 */
class BudgetTierPacketExtractor(context: Context) :
    PacketExtractor by LlmPacketExtractor(
        context = context,
        modelPath = ModelPaths.budgetTierModel(context).absolutePath,
        tier = DeviceTier.BUDGET,
        maxTokens = 512
    )

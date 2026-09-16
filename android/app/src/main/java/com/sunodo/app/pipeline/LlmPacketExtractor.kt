package com.sunodo.app.pipeline

import android.content.Context
import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.AudioModelOptions
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared MediaPipe Tasks GenAI session handling for both tiers — see
 * HighTierPacketExtractor and BudgetTierPacketExtractor, which are thin
 * wrappers around this pointed at different model checkpoints.
 *
 * UPDATED after live web research (see the commit this landed in): the
 * audio call was originally a guessed `session.addAudioClip(pcm)` — wrong.
 * Confirmed against Google's official Android LLM Inference guide
 * (developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android)
 * as of this fix: the real call is `session.addAudio(audioData: ByteArray)`,
 * gated behind `.setAudioModelOptions(...)` on the engine options AND
 * `.setGraphOptions(GraphOptions.builder().setEnableAudioModality(true)...)`
 * on the session options — both now wired in below, neither of which
 * existed in the original version of this file at all, not just the method
 * name. Gemma's own audio docs (ai.google.dev/gemma/docs/capabilities/audio)
 * and flutter_gemma's AudioPromptPart docs both independently confirm the
 * expected byte format: PCM16, 16kHz, mono — which is what
 * AudioPreprocessor.preparePcm16Mono16kHz produces.
 *
 * Two things confirmed by that same research matter more than any single
 * method name, and are NOT yet reflected in ModelPaths.kt's file naming —
 * see android/README.md's "Open questions" section:
 * (1) the commonly-linked gemma-3n-E2B-it-int4.task "preview" checkpoint is
 *     text-and-vision only — no audio — regardless of what this code calls.
 *     The audio-capable checkpoint is a newer .litertlm-format release
 *     (e.g. google/gemma-3n-E2B-it-litert-lm on Hugging Face).
 * (2) Google's own docs state the LLM Inference API is optimized for
 *     high-end physical devices and does not reliably support emulators.
 *
 * None of this file has been compiled against a real MediaPipe classpath or
 * run on a device from this sandbox (no route to Google's Maven repository
 * here — see the root README's constraints section). The method names and
 * required options below are now sourced from Google's current official
 * docs rather than guessed, which is a meaningfully different confidence
 * level than before — but "confirmed by reading the docs" is still not the
 * same as "confirmed by compiling it."
 */
class LlmPacketExtractor(
    private val context: Context,
    private val modelPath: String,
    private val tier: DeviceTier,
    private val maxTokens: Int
) : PacketExtractor {

    override suspend fun extract(input: VoiceInput, sourceApp: String, durationSec: Int): ExtractionResult =
        when (input) {
            is VoiceInput.Transcript -> extractFromText(input.text)
            is VoiceInput.Audio -> extractFromAudio(input.uri)
        }

    private suspend fun extractFromText(transcript: String): ExtractionResult {
        val prompt = PacketPrompt.systemInstruction() + "\n\nTranscript:\n" + transcript
        val raw = runInference(useAudioModality = false) { session -> session.addQueryChunk(prompt) }
        return PacketJsonParser.parse(raw, tier)
    }

    private suspend fun extractFromAudio(uri: Uri): ExtractionResult {
        val raw = runInference(useAudioModality = true) { session ->
            session.addQueryChunk(PacketPrompt.audioInstruction())
            val audioBytes = AudioPreprocessor.preparePcm16Mono16kHz(context, uri)
            session.addAudio(audioBytes)
        }
        return PacketJsonParser.parse(raw, tier)
    }

    private suspend fun runInference(
        useAudioModality: Boolean,
        feed: (LlmInferenceSession) -> Unit
    ): String = withContext(Dispatchers.Default) {
        val engineOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(maxTokens)
            .apply {
                if (useAudioModality) setAudioModelOptions(AudioModelOptions.builder().build())
            }
            .build()
        val llmInference = LlmInference.createFromOptions(context, engineOptions)

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(0.2f)
            .setTopK(40)
            .apply {
                if (useAudioModality) {
                    setGraphOptions(GraphOptions.builder().setEnableAudioModality(true).build())
                }
            }
            .build()
        val session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)

        try {
            feed(session)
            session.generateResponse()
        } finally {
            session.close()
            llmInference.close()
        }
    }
}

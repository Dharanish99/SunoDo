package com.sunodo.app.pipeline

import android.content.Context
import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared MediaPipe Tasks GenAI session handling for both tiers — see
 * HighTierPacketExtractor and BudgetTierPacketExtractor, which are thin
 * wrappers around this pointed at different model checkpoints.
 *
 * Confidence level, file by file is not uniform, so read this before relying
 * on it: LlmInference.createFromOptions / LlmInferenceSession.createFromOptions
 * / addQueryChunk / generateResponse match MediaPipe's stable, documented
 * text-generation API shape with reasonable confidence. The audio path does
 * not: `session.addAudioClip(pcm)` below is a TODO(verify) placeholder, not
 * a confirmed method name. Gemma-3n audio ingestion is the newest, fastest-
 * moving part of this whole stack — docs/blueprint.md itself flags that the
 * plain LLM Inference API is already in maintenance mode in favor of
 * LiteRT-LM — so check the exact call against MediaPipe's current docs or
 * the Google AI Edge Gallery reference app before trusting it. None of this
 * file has been compiled against a real MediaPipe classpath or run on a
 * device from this sandbox (no route to Google's Maven repository here —
 * see the root README's constraints section).
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
        val raw = runInference { session -> session.addQueryChunk(prompt) }
        return PacketJsonParser.parse(raw, tier)
    }

    private suspend fun extractFromAudio(uri: Uri): ExtractionResult {
        // MediaPipe Tasks GenAI currently does not have an audio ingestion API (`addAudioClip`).
        // Once audio ingestion (LiteRT-LM / MediaPipe) is released, pass PCM from AudioPreprocessor:
        // AudioPreprocessor.decodeToPcm16(context, uri)
        throw UnsupportedOperationException(
            "Direct audio ingestion is not supported by the current MediaPipe Tasks GenAI version. " +
                "Use VoiceInput.Transcript or wait for LiteRT audio ingestion API."
        )
    }

    private suspend fun runInference(feed: (LlmInferenceSession) -> Unit): String =
        withContext(Dispatchers.Default) {
            val llmInference = LlmInference.createFromOptions(
                context,
                LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(maxTokens)
                    .build()
            )
            val session = LlmInferenceSession.createFromOptions(
                llmInference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTemperature(0.2f)
                    .setTopK(40)
                    .build()
            )
            try {
                feed(session)
                session.generateResponse()
            } finally {
                session.close()
                llmInference.close()
            }
        }
}

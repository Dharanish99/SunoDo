package com.sunodo.app.pipeline

import com.sunodo.app.data.PacketType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Turns the model's raw text response into an ExtractionResult, matching
 * the schema in PacketPrompt. Uses org.json, which ships as part of the
 * Android platform SDK — no extra Gradle dependency needed, but it also
 * means this file can't be exercised by a plain-JVM unit test the way
 * AudioPreprocessor's pure chunk math could be.
 *
 * org.json quirk this code relies on: when a JSON field is explicitly
 * `null`, optString(key, default) returns the literal string "null" rather
 * than Kotlin null (JSONObject.NULL.toString() == "null") — so a JSON-null
 * value and an absent key are NOT the same case, and both have to be
 * checked for.
 */
object PacketJsonParser {

    fun parse(rawModelOutput: String, tier: DeviceTier): ExtractionResult {
        val json = JSONObject(stripCodeFences(rawModelOutput))
        val tldr = json.optString("tldr", "Voice note processed.")
        val packetsJson: JSONArray = json.optJSONArray("packets") ?: JSONArray()

        val packets = mutableListOf<ExtractedPacket>()
        for (i in 0 until packetsJson.length()) {
            val p = packetsJson.optJSONObject(i) ?: continue
            val content = p.optString("content").ifBlank { continue }

            val type = when (p.optString("type", "info").lowercase()) {
                "task" -> PacketType.TASK
                "question" -> PacketType.QUESTION
                "decision" -> PacketType.DECISION
                else -> PacketType.INFO
            }

            val dueRaw = p.optString("due", "null")
            val dueEpochDay = dueRaw.takeIf { it.isNotBlank() && it != "null" }
                ?.let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }

            val replyRaw = p.optString("reply_suggestion", "null")
            val reply = replyRaw.takeIf { it.isNotBlank() && it != "null" }

            packets.add(ExtractedPacket(type, content, dueEpochDay, reply))
        }

        return ExtractionResult(tldr = tldr, packets = packets, deviceTier = tier)
    }

    private fun stripCodeFences(text: String): String =
        text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}

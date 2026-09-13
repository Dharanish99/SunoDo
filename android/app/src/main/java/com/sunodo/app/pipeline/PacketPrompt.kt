package com.sunodo.app.pipeline

import java.time.LocalDate

/**
 * The extraction schema both tiers structure their output against — kept
 * word-for-word equivalent to the SYSTEM_PROMPT in demo/sunodo_demo.html so
 * the Android app and the Track B web demo are provably the same idea, not
 * two different ones that happen to look similar.
 */
object PacketPrompt {

    fun systemInstruction(): String {
        val today = LocalDate.now()
        return """
            You are SunoDo's on-device extraction engine. You receive a transcript of a voice note and must extract discrete, atomic action packets from it — never a running summary of everything said, only the pieces someone would actually need to act on.

            Today's date is $today. Resolve relative dates ("Tuesday", "the 15th", "next Monday") against that date and always output resolved dates in YYYY-MM-DD format.

            Respond with ONLY a single JSON object and nothing else — no markdown fences, no preamble, no trailing commentary. Match exactly this shape:

            {
              "tldr": "one sentence, max 20 words, plain language",
              "packets": [
                {
                  "type": "task" | "question" | "decision" | "info",
                  "content": "short, specific, in plain language",
                  "due": "YYYY-MM-DD or null",
                  "reply_suggestion": "a short one-line reply the recipient could send back — only for type question, otherwise null"
                }
              ]
            }

            Extract between 2 and 5 packets. Do not invent details that are not in the transcript.
        """.trimIndent()
    }

    /** Used only by the audio-native (high-tier) path, which has no separate transcript to hand over. */
    fun audioInstruction(): String =
        systemInstruction() + "\n\nThe voice note's audio follows this instruction. Transcribe it internally and extract packets directly from it — do not output the transcript itself."
}

package com.sunodo.app.pipeline

import android.net.Uri

/**
 * What a PacketExtractor is handed. The two tiers genuinely take different
 * shapes of input by design (blueprint.md §3.3): the high-tier path ingests
 * raw audio directly in one pass, the budget-tier path needs a transcript
 * first. `Transcript` also covers the "try a sample" / pasted-text flows,
 * which never had audio to begin with.
 */
sealed interface VoiceInput {
    data class Audio(val uri: Uri) : VoiceInput
    data class Transcript(val text: String) : VoiceInput
}

package com.sunodo.app.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import com.sunodo.app.pipeline.VoiceInput
import com.sunodo.app.ui.SAMPLE_TRANSCRIPT
import com.sunodo.app.ui.SunoDoScreen
import com.sunodo.app.ui.theme.SunoDoTheme

/**
 * The one integration point with any other app — registered in
 * AndroidManifest.xml as a Share target for any audio MIME type
 * (docs/blueprint.md's ShareIntentReceiver module). Reading the shared audio's display name below
 * only uses the transient URI permission grant attached to this Intent;
 * no storage permission is declared or needed anywhere in this app.
 *
 * Stage 2 does not run real speech-to-text yet — that begins in Stage 3 —
 * so the extraction itself still runs on the same stub transcript as
 * "try a sample" on the home screen. What's real here is the OS integration:
 * this activity is genuinely reachable from WhatsApp's share sheet today,
 * genuinely receives the audio file, and genuinely reads its name.
 */
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val audioUri: Uri? = if (intent?.action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            null
        }
        val displayName = audioUri?.let { queryDisplayName(it) }
        val autoStartInput = audioUri?.let { VoiceInput.Audio(it) } ?: VoiceInput.Transcript(SAMPLE_TRANSCRIPT)

        setContent {
            SunoDoTheme {
                SunoDoScreen(
                    receivedLabel = displayName?.let { "Received: $it" } ?: "Received a voice note",
                    autoStartInput = autoStartInput,
                    onPacketAction = {
                        Toast.makeText(
                            this,
                            "Real Calendar / Reminder / Reply intents arrive in Stage 4 (OSActionBridge)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    } catch (e: SecurityException) {
        null
    }
}

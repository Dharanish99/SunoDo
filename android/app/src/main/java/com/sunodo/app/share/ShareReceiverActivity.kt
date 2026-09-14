package com.sunodo.app.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import com.sunodo.app.actions.PacketActionHandler
import com.sunodo.app.pipeline.VoiceInput
import com.sunodo.app.ui.SunoDoScreen
import com.sunodo.app.ui.theme.SunoDoTheme

/**
 * The one integration point with any other app — registered in
 * AndroidManifest.xml as a Share target for any audio MIME type
 * (docs/blueprint.md's ShareIntentReceiver module). Reading the shared
 * audio's display name below only uses the transient URI permission grant
 * attached to this Intent; no storage permission is declared or needed
 * anywhere in this app.
 *
 * If the incoming Intent doesn't actually carry a readable audio URI —
 * malformed share, revoked permission, an app that mislabels its MIME type —
 * this reports a genuine error (Stage 5) instead of silently substituting
 * the sample transcript, which earlier stages did and which would have
 * quietly shown someone a stranger's Q3-report note in place of whatever
 * they actually tried to share.
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

        setContent {
            SunoDoTheme {
                SunoDoScreen(
                    receivedLabel = displayName?.let { "Received: $it" } ?: "Received a voice note",
                    autoStartInput = audioUri?.let { VoiceInput.Audio(it) },
                    autoStartError = if (audioUri == null) {
                        "Didn't receive an audio file with that share — try again from the app you shared it from."
                    } else {
                        null
                    },
                    onPacketAction = { packet -> PacketActionHandler.handle(this, packet) }
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    } catch (e: Exception) {
        null
    }
}

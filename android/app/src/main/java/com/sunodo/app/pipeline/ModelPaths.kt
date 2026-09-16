package com.sunodo.app.pipeline

import android.content.Context
import java.io.File

/**
 * Model files are hundreds of MB to multiple GB and are never committed to
 * this repo. During development they need to be pushed onto the device by hand.
 *
 * UPDATED after live web research while fixing LlmPacketExtractor.kt's
 * audio call: the high-tier filename below changed from
 * gemma-3n-e2b-it-int4.task to a .litertlm file on purpose. The commonly
 * linked gemma-3n-E2B-it-int4.task "preview" checkpoint (the one most
 * download guides point to first) is confirmed text-and-vision only — no
 * audio — regardless of what the code calls. The audio-capable release is
 * a newer .litertlm-format checkpoint, e.g.
 * google/gemma-3n-E2B-it-litert-lm on Hugging Face. Get that one, not the
 * older preview .task file, or the audio path will fail even with a
 * correct addAudio() call. See android/README.md's "Getting a real
 * extraction running" section for the current download + push steps.
 *
 *   adb push gemma-3n-e2b-it-int4.litertlm \
 *     /sdcard/Android/data/com.sunodo.app/files/models/gemma-3n-e2b-it-int4.litertlm
 *
 *   adb push gemma3-1b-it-int4.task \
 *     /sdcard/Android/data/com.sunodo.app/files/models/gemma3-1b-it-int4.task
 *
 * The budget-tier filename is UNCHANGED and that's a real open problem, not
 * an oversight: Gemma 3 1B doesn't appear to have an audio-capable
 * checkpoint at all (unlike Gemma-3n), so BudgetTierPacketExtractor's
 * audio path is unverified in a different, more fundamental way than the
 * high-tier one — see android/README.md's "Open questions" section.
 *
 * A production build would replace all of this with an in-app, Wi-Fi-gated
 * download flow instead of expecting a developer to adb push a file — out
 * of scope here.
 */
object ModelPaths {
    private const val MODELS_DIR = "models"

    fun highTierModel(context: Context): File = modelsDir(context).resolve("gemma-3n-e2b-it-int4.litertlm")
    fun budgetTierModel(context: Context): File = modelsDir(context).resolve("gemma3-1b-it-int4.task")
    fun modelsDir(context: Context): File = File(context.getExternalFilesDir(null), MODELS_DIR)

    fun isHighTierModelPresent(context: Context): Boolean = highTierModel(context).exists()
    fun isBudgetTierModelPresent(context: Context): Boolean = budgetTierModel(context).exists()
}

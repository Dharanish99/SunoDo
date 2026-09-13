package com.sunodo.app.pipeline

import android.content.Context
import java.io.File

/**
 * Model files are hundreds of MB to multiple GB and are never committed to
 * this repo. During development they need to be pushed onto the device by hand, e.g.:
 *
 *   adb push gemma-3n-e2b-it-int4.task \
 *     /sdcard/Android/data/com.sunodo.app/files/models/gemma-3n-e2b-it-int4.task
 *
 *   adb push gemma3-1b-it-int4.task \
 *     /sdcard/Android/data/com.sunodo.app/files/models/gemma3-1b-it-int4.task
 *
 * A production build would replace this with an in-app, Wi-Fi-gated download
 * flow instead of expecting a developer to adb push a file — out of scope here.
 */
object ModelPaths {
    private const val MODELS_DIR = "models"

    fun highTierModel(context: Context): File = modelsDir(context).resolve("gemma-3n-e2b-it-int4.task")
    fun budgetTierModel(context: Context): File = modelsDir(context).resolve("gemma3-1b-it-int4.task")
    fun modelsDir(context: Context): File = File(context.getExternalFilesDir(null), MODELS_DIR)

    fun isHighTierModelPresent(context: Context): Boolean = highTierModel(context).exists()
    fun isBudgetTierModelPresent(context: Context): Boolean = budgetTierModel(context).exists()
}

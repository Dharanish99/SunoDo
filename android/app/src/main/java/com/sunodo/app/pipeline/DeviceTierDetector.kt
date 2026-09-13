package com.sunodo.app.pipeline

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences

/**
 * The one piece of Stage 3 that needed no model file to be real and
 * testable today (docs/blueprint.md §3.4's "validated constraint" — Gemma-3n
 * E2B's ~5.9GB peak footprint doesn't run smoothly under 6GB of RAM).
 * Runs once, caches the result, exactly as the design doc specifies.
 */
object DeviceTierDetector {
    private const val PREFS_NAME = "sunodo_device_tier"
    private const val KEY_TIER = "cached_tier"
    private const val HIGH_TIER_MIN_RAM_MB = 6000L

    fun detect(context: Context): DeviceTier {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_TIER, null)?.let { runCatching { DeviceTier.valueOf(it) }.getOrNull() }
        return cached ?: computeAndCache(context, prefs)
    }

    private fun computeAndCache(context: Context, prefs: SharedPreferences): DeviceTier {
        val tier = computeTier(context)
        prefs.edit().putString(KEY_TIER, tier.name).apply()
        return tier
    }

    private fun computeTier(context: Context): DeviceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
        return if (totalRamMb >= HIGH_TIER_MIN_RAM_MB) DeviceTier.HIGH else DeviceTier.BUDGET
    }
}

package com.sunodo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per voice note that has been through the pipeline.
 * `deviceTier` is recorded (not just used transiently) so the adaptive
 * device-tier logic from docs/blueprint.md §3.4 is auditable after the fact —
 * useful for debugging which path a given note actually took.
 */
@Entity(tableName = "voice_notes")
data class VoiceNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceApp: String,
    val durationSec: Int,
    val tldr: String,
    val createdAtEpochMillis: Long,
    val deviceTier: String
)

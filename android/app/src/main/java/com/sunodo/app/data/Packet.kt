package com.sunodo.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One atomic, independently-actionable unit extracted from a voice note.
 * `dueEpochDay` stores java.time.LocalDate.toEpochDay() so date math and
 * sorting stay simple without pulling in a date-string parser at read time.
 */
@Entity(
    tableName = "packets",
    foreignKeys = [
        ForeignKey(
            entity = VoiceNote::class,
            parentColumns = ["id"],
            childColumns = ["voiceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("voiceNoteId")]
)
data class Packet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voiceNoteId: Long,
    val type: PacketType,
    val content: String,
    val dueEpochDay: Long?,
    val replySuggestion: String?,
    val dismissed: Boolean = false
)

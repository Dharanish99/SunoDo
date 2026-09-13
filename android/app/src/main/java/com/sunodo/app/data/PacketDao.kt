package com.sunodo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PacketDao {

    @Insert
    suspend fun insertVoiceNote(voiceNote: VoiceNote): Long

    @Insert
    suspend fun insertPackets(packets: List<Packet>)

    /**
     * Room supports a default-bodied @Transaction method on a Kotlin DAO
     * interface: it calls the two abstract methods above, and Room wraps
     * both writes in one transaction without needing a generated override.
     */
    @Transaction
    suspend fun insertVoiceNoteWithPackets(voiceNote: VoiceNote, packets: List<Packet>): Long {
        val voiceNoteId = insertVoiceNote(voiceNote)
        insertPackets(packets.map { it.copy(voiceNoteId = voiceNoteId) })
        return voiceNoteId
    }

    @Query("SELECT * FROM voice_notes ORDER BY createdAtEpochMillis DESC")
    fun observeVoiceNotes(): Flow<List<VoiceNote>>

    @Query("SELECT * FROM packets WHERE voiceNoteId = :voiceNoteId AND dismissed = 0")
    fun observePackets(voiceNoteId: Long): Flow<List<Packet>>

    @Query("UPDATE packets SET dismissed = 1 WHERE id = :packetId")
    suspend fun dismissPacket(packetId: Long)
}

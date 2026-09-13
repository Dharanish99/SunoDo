package com.sunodo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [VoiceNote::class, Packet::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SunoDoDatabase : RoomDatabase() {

    abstract fun packetDao(): PacketDao

    companion object {
        @Volatile private var INSTANCE: SunoDoDatabase? = null

        fun getInstance(context: Context): SunoDoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SunoDoDatabase::class.java,
                    "sunodo.db"
                ).build().also { INSTANCE = it }
            }
    }
}

package com.sunodo.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPacketType(value: PacketType): String = value.name

    @TypeConverter
    fun toPacketType(value: String): PacketType = PacketType.valueOf(value)
}

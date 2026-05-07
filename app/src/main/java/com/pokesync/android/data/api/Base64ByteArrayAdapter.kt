package com.pokesync.android.data.api

import android.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * Handles byte arrays serialized as either Base64 strings (C# System.Text.Json)
 * or JSON arrays (standard Moshi behavior).
 */
class Base64ByteArrayAdapter : JsonAdapter<ByteArray?>() {

    override fun fromJson(reader: JsonReader): ByteArray? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> reader.nextNull()
            JsonReader.Token.STRING -> Base64.decode(reader.nextString(), Base64.DEFAULT)
            else -> {
                val list = mutableListOf<Byte>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(reader.nextInt().toByte())
                }
                reader.endArray()
                list.toByteArray()
            }
        }
    }

    override fun toJson(writer: JsonWriter, value: ByteArray?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginArray()
        for (b in value) {
            writer.value(b.toInt() and 0xFF)
        }
        writer.endArray()
    }
}

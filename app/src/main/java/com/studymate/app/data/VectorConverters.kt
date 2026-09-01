package com.studymate.app.data

import androidx.room.TypeConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Room TypeConverters for storing a [FloatArray] (embedding vector) compactly as a BLOB.
 *
 * We use 4-byte IEEE-754 floats (not double) to keep memory low on 1GB-RAM devices:
 * a 384-dim embedding = 384 * 4 = 1.5 KB per chunk.
 */
class VectorConverters {

    @TypeConverter
    fun floatArrayToBlob(value: FloatArray?): ByteArray? {
        if (value == null) return null
        val baos = ByteArrayOutputStream(value.size * 4)
        DataOutputStream(baos).use { dos ->
            dos.writeInt(value.size)
            for (f in value) dos.writeFloat(f)
        }
        return baos.toByteArray()
    }

    @TypeConverter
    fun blobToFloatArray(blob: ByteArray?): FloatArray? {
        if (blob == null) return null
        DataInputStream(ByteArrayInputStream(blob)).use { dis ->
            val size = dis.readInt()
            val arr = FloatArray(size)
            for (i in 0 until size) arr[i] = dis.readFloat()
            return arr
        }
    }
}

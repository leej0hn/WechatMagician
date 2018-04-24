package com.gh0u1l5.wechatmagician.spellbook.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock.elapsedRealtime
import java.io.*
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.*

// FileUtil is a helper object for file I/O.
object FileUtil {
    // writeBytesToDisk writes the given bytes to specific path.
    fun writeBytesToDisk(path: String, content: ByteArray) {
        val file = File(path)
        file.parentFile.mkdirs()
        FileOutputStream(file).use {
            it.write(content)
        }
    }

    // readBytesFromDisk returns all the bytes of a binary file.
    fun readBytesFromDisk(path: String): ByteArray {
        return FileInputStream(path).use {
            it.readBytes()
        }
    }

    // writeObjectToDisk writes a serializable object to disk.
    fun writeObjectToDisk(path: String, obj: Serializable) {
        val out = ByteArrayOutputStream()
        ObjectOutputStream(out).use {
            it.writeObject(obj)
        }
        writeBytesToDisk(path, out.toByteArray())
    }

    // readObjectFromDisk reads a serializable object from disk.
    fun readObjectFromDisk(path: String): Any? {
        val bytes = readBytesFromDisk(path)
        val ins = ByteArrayInputStream(bytes)
        return ObjectInputStream(ins).use {
            it.readObject()
        }
    }

    fun writeInputStreamToDisk(path: String, `in`: InputStream, bufferSize: Int = 8192) {
        val file = File(path)
        file.parentFile.mkdirs()
        FileOutputStream(file).use {
            val buffer = ByteArray(bufferSize)
            var length = `in`.read(buffer)
            while (length != -1) {
                it.write(buffer, 0, length)
                length = `in`.read(buffer)
            }
        }
    }

    // writeBitmapToDisk writes the given bitmap to disk.
    fun writeBitmapToDisk(path: String, bitmap: Bitmap) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        writeBytesToDisk(path, out.toByteArray())
    }

    // writeOnce ensures that the writeCallback will only be executed once for each boot.
    fun writeOnce(path: String, writeCallback: (String) -> Unit) {
        val file = File(path)
        if (!file.exists()) {
            writeCallback(path)
            return
        }
        val bootAt = currentTimeMillis() - elapsedRealtime()
        val modifiedAt = file.lastModified()
        if (modifiedAt < bootAt) {
            writeCallback(path)
        }
    }

    // createTimeTag returns the current time in a simple format as a time tag.
    private val formatter = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.getDefault())
    fun createTimeTag(): String = formatter.format(Calendar.getInstance().time)

    // notifyNewMediaFile notifies all the gallery apps that there is a new file to scan.
    fun notifyNewMediaFile(path: String, context: Context?) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        context?.sendBroadcast(intent.apply {
            data = Uri.fromFile(File(path))
        })
    }
}

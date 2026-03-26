package org.fossify.gallery.helpers

import android.content.Context
import android.net.Uri
import org.apache.sanselan.common.byteSources.ByteSourceInputStream
import org.apache.sanselan.formats.jpeg.JpegImageParser
import java.io.File
import java.io.RandomAccessFile
import androidx.core.net.toUri

data class MotionPhotoInfo(
    val videoOffsetFromStart: Long,
    val videoLength: Long
)

object MotionPhotoHelper {

    private const val SCAN_RANGE = 5L * 1024 * 1024

    private val FTYP_MARKER = "ftyp".toByteArray(Charsets.US_ASCII)

    fun detectMotionPhoto(context: Context, path: String, name: String): MotionPhotoInfo? {
        if (!name.endsWith(".jpg", true) && !name.endsWith(".jpeg", true)) {
            return null
        }

        val xmpXml = try {
            val inputStream = if (path.startsWith("content:/")) {
                context.contentResolver.openInputStream(path.toUri())
            } else {
                File(path).inputStream()
            }
            inputStream?.use {
                JpegImageParser().getXmpXml(ByteSourceInputStream(it, name), HashMap<String, Any>())
            }
        } catch (_: OutOfMemoryError) {
            null
        }

        if (xmpXml == null) return null

        val isMotionPhoto = xmpXml.contains("GCamera:MotionPhoto=\"1\"", true) ||
            xmpXml.contains("<GCamera:MotionPhoto>1</GCamera:MotionPhoto>", true)

        if (!isMotionPhoto) return null

        return findVideoOffset(context, path)
    }

    private fun findVideoOffset(context: Context, path: String): MotionPhotoInfo? {
        return if (path.startsWith("content:/")) {
            findVideoOffsetFromContentUri(context, path)
        } else {
            findVideoOffsetFromFile(path)
        }
    }

    private fun findVideoOffsetFromFile(path: String): MotionPhotoInfo? {
        val file = File(path)
        val fileSize = file.length()
        if (fileSize < 12) return null

        val scanStart = maxOf(0L, fileSize - SCAN_RANGE)
        val scanLength = (fileSize - scanStart).toInt()

        RandomAccessFile(file, "r").use { raf ->
            raf.seek(scanStart)
            val buffer = ByteArray(scanLength)
            raf.readFully(buffer)
            val relativeOffset = findFtypOffset(buffer) ?: return null
            val absoluteOffset = scanStart + relativeOffset
            return MotionPhotoInfo(
                videoOffsetFromStart = absoluteOffset,
                videoLength = fileSize - absoluteOffset
            )
        }
    }

    private fun findVideoOffsetFromContentUri(context: Context, path: String): MotionPhotoInfo? {
        val uri = Uri.parse(path)
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        return pfd.use {
            val fileSize = it.statSize
            if (fileSize < 12) return null

            val scanStart = maxOf(0L, fileSize - SCAN_RANGE)
            val scanLength = (fileSize - scanStart).toInt()

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            inputStream.use { stream ->
                stream.skip(scanStart)
                val buffer = ByteArray(scanLength)
                var totalRead = 0
                while (totalRead < scanLength) {
                    val read = stream.read(buffer, totalRead, scanLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                val relativeOffset = findFtypOffset(buffer) ?: return null
                val absoluteOffset = scanStart + relativeOffset
                MotionPhotoInfo(
                    videoOffsetFromStart = absoluteOffset,
                    videoLength = fileSize - absoluteOffset
                )
            }
        }
    }

    private fun findFtypOffset(buffer: ByteArray): Int? {
        // Search for "ftyp" marker and validate it's an MP4 box header.
        // The box structure is: [4 bytes size][4 bytes "ftyp"][4+ bytes brand]
        // So we look for "ftyp" at position i, and the box starts at i-4.
        for (i in 4 until buffer.size - 4) {
            if (matchesFtypMarker(buffer, i)) {
                val boxStart = i - 4
                val boxSize = ((buffer[boxStart].toInt() and 0xFF) shl 24) or
                    ((buffer[boxStart + 1].toInt() and 0xFF) shl 16) or
                    ((buffer[boxStart + 2].toInt() and 0xFF) shl 8) or
                    (buffer[boxStart + 3].toInt() and 0xFF)
                if (boxSize in 8..64) {
                    return boxStart
                }
            }
        }
        return null
    }

    private fun matchesFtypMarker(buffer: ByteArray, i: Int): Boolean = buffer[i] == FTYP_MARKER[0] &&
        buffer[i + 1] == FTYP_MARKER[1] &&
        buffer[i + 2] == FTYP_MARKER[2] &&
        buffer[i + 3] == FTYP_MARKER[3]
}

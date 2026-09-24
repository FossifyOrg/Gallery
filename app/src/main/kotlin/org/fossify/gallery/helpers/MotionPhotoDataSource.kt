@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package org.fossify.gallery.helpers

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.RandomAccessFile
import androidx.core.net.toUri

class MotionPhotoDataSource(
    private val context: Context,
    private val filePath: String,
    private val videoOffset: Long,
    private val videoLength: Long
) : BaseDataSource(false) {

    private var randomAccessFile: RandomAccessFile? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: java.io.InputStream? = null
    private var bytesRemaining = 0L
    private var opened = false
    private var uri: Uri? = null

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val position = dataSpec.position
        val requestedLength = dataSpec.length

        val effectiveLength = if (requestedLength != C.LENGTH_UNSET.toLong()) {
            minOf(requestedLength, videoLength - position)
        } else {
            videoLength - position
        }

        if (filePath.startsWith("content:/")) {
            val pfd = context.contentResolver.openFileDescriptor(filePath.toUri(), "r")
                ?: throw java.io.FileNotFoundException("Cannot open $filePath")
            parcelFileDescriptor = pfd
            val fis = java.io.FileInputStream(pfd.fileDescriptor)
            fis.skip(videoOffset + position)
            inputStream = fis
        } else {
            val raf = RandomAccessFile(File(filePath), "r")
            raf.seek(videoOffset + position)
            randomAccessFile = raf
        }

        bytesRemaining = effectiveLength
        opened = true
        transferStarted(dataSpec)
        return effectiveLength
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining <= 0) return C.RESULT_END_OF_INPUT

        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = randomAccessFile?.read(buffer, offset, toRead)
            ?: inputStream?.read(buffer, offset, toRead)
            ?: return C.RESULT_END_OF_INPUT

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try {
            randomAccessFile?.close()
        } finally {
            randomAccessFile = null
        }
        try {
            inputStream?.close()
        } finally {
            inputStream = null
        }
        try {
            parcelFileDescriptor?.close()
        } finally {
            parcelFileDescriptor = null
        }
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}

class MotionPhotoDataSourceFactory(
    private val context: Context,
    private val filePath: String,
    private val videoOffset: Long,
    private val videoLength: Long
) : DataSource.Factory {
    override fun createDataSource(): MotionPhotoDataSource {
        return MotionPhotoDataSource(context, filePath, videoOffset, videoLength)
    }
}

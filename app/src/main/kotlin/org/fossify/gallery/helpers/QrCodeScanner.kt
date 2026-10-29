package org.fossify.gallery.helpers

import android.content.Context
import android.net.Uri
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/** Loads local images for offline QR decoding. */
object QrCodeScanner {
    private const val MAX_IMAGE_SIZE = 2048

    /** Blocks the calling worker thread; loading failures propagate instead of returning no matches. */
    fun scan(context: Context, source: String): List<String> {
        check(Looper.myLooper() != Looper.getMainLooper()) { "QR scanning requires a worker thread" }
        if (Thread.currentThread().isInterrupted) {
            throw CancellationException("QR scanning interrupted")
        }

        val uri = Uri.parse(source)
        val model = when (uri.scheme?.lowercase()) {
            "content", "file" -> uri
            null -> File(source)
            else -> throw IllegalArgumentException("QR scanning only supports local images")
        }
        val requests = Glide.with(context.applicationContext)
        val target = requests.asBitmap()
            .load(model)
            .disallowHardwareConfig()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()
            .submit(MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)

        try {
            val bitmap = target.get(15, TimeUnit.SECONDS)
            if (Thread.currentThread().isInterrupted) {
                throw CancellationException("QR scanning interrupted")
            }

            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return QrCodeDecoder.decode(pixels, bitmap.width, bitmap.height)
        } finally {
            requests.clear(target)
        }
    }
}

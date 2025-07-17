package org.fossify.gallery.helpers

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder

class PicassoRegionDecoder(
    val showHighestQuality: Boolean,
    val screenWidth: Int,
    val screenHeight: Int,
    val minTileDpi: Int,
    private val orientation: Int
) : ImageRegionDecoder {
    private var decoder: BitmapRegionDecoder? = null
    private val decoderLock = Any()

    override fun init(context: Context, uri: Uri): Point {
        val newUri = Uri.parse(uri.toString().replace("%", "%25").replace("#", "%23"))
        val inputStream = context.contentResolver.openInputStream(newUri)
        decoder = BitmapRegionDecoder.newInstance(inputStream!!, false)
        return Point(decoder!!.width, decoder!!.height)
    }

    override fun decodeRegion(rect: Rect, sampleSize: Int): Bitmap {
        synchronized(decoderLock) {
            var newSampleSize = sampleSize
            if (!showHighestQuality && minTileDpi == LOW_TILE_DPI) {
                if ((rect.width() > rect.height() && screenWidth > screenHeight) || (rect.height() > rect.width() && screenHeight > screenWidth)) {
                    if ((rect.width() / sampleSize > screenWidth || rect.height() / sampleSize > screenHeight)) {
                        newSampleSize *= 2
                    }
                }
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = newSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            var bitmap = decoder!!.decodeRegion(rect, options)
            if (bitmap == null) {
                throw RuntimeException("Region decoder returned null bitmap - image format may not be supported")
            }

            // Apply EXIF mirror flips (orientation 2,4,5,7)
            val needsFlipHorizontal = orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL || orientation == ExifInterface.ORIENTATION_TRANSPOSE || orientation == ExifInterface.ORIENTATION_TRANSVERSE
            val needsFlipVertical = orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL

            if (needsFlipHorizontal || needsFlipVertical) {
                val matrix = Matrix().apply {
                    preScale(if (needsFlipHorizontal) -1f else 1f, if (needsFlipVertical) -1f else 1f)
                    if (needsFlipHorizontal) {
                        postTranslate(bitmap.width.toFloat(), 0f)
                    }
                    if (needsFlipVertical) {
                        postTranslate(0f, bitmap.height.toFloat())
                    }
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            return bitmap
        }
    }

    override fun isReady() = decoder != null && !decoder!!.isRecycled

    override fun recycle() {
        decoder!!.recycle()
    }
}

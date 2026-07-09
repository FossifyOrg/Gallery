package org.fossify.gallery.helpers

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE
import androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder

class PicassoRegionDecoder(
    val showHighestQuality: Boolean,
    val screenWidth: Int,
    val screenHeight: Int,
    val minTileDpi: Int,
    val orientation: Int,
) : ImageRegionDecoder {
    private var decoder: BitmapRegionDecoder? = null
    private val decoderLock = Any()

    override fun init(context: Context, uri: Uri): Point {
        val newUri = Uri.parse(uri.toString().replace("%", "%25").replace("#", "%23"))
        val inputStream = context.contentResolver.openInputStream(newUri)
        decoder = BitmapRegionDecoder.newInstance(inputStream!!, false)
        val whSwapped = OrientationTransformation.whSwapped(orientation)
        val w = if (whSwapped) decoder!!.height else decoder!!.width
        val h = if (whSwapped) decoder!!.width else decoder!!.height
        return Point(w, h)
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

            // To respect EXIF orientation, we must treat `rect` as a region of the transformed image.
            // To get the region in the raw, untransformed image, we must first apply the inverse of the EXIF orientation transformation.
            val rectInRaw = when (orientation) {
                ORIENTATION_NORMAL -> rect
                ORIENTATION_ROTATE_270 -> {
                    Rect(decoder!!.width - rect.bottom, rect.left, decoder!!.width - rect.top, rect.right)
                }
                ORIENTATION_TRANSPOSE -> {
                    Rect(rect.top, rect.left, rect.bottom, rect.right)
                }
                ORIENTATION_ROTATE_180 -> {
                    Rect(decoder!!.width - rect.right, decoder!!.height - rect.bottom, decoder!!.width - rect.left, decoder!!.height - rect.top)
                }
                ORIENTATION_ROTATE_90 -> {
                    Rect(rect.top, decoder!!.height - rect.right, rect.bottom, decoder!!.height - rect.left)
                }
                ORIENTATION_TRANSVERSE -> {
                    Rect(decoder!!.width - rect.right, decoder!!.height - rect.bottom, decoder!!.width - rect.left, decoder!!.height - rect.top)
                }
                ORIENTATION_FLIP_HORIZONTAL -> {
                    Rect(decoder!!.width - rect.right, rect.top, decoder!!.width - rect.left, rect.bottom)
                }
                ORIENTATION_FLIP_VERTICAL -> {
                    Rect(rect.left, decoder!!.height - rect.bottom, rect.right, decoder!!.height - rect.top)
                }
                else -> rect
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = newSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmapInRaw = decoder!!.decodeRegion(rectInRaw, options)
                ?: throw RuntimeException("Region decoder returned null bitmap - image format may not be supported")

            // `bitmapInRaw` is now the correct region out of the transformed image, but is not itself transformed.
            // To fix this, we apply the transformation as normal to the region bitmap.
            val orientationMatrix = OrientationTransformation.getMatrix(orientation)
            return Bitmap.createBitmap(bitmapInRaw, 0, 0, rect.width() / newSampleSize, rect.height() / newSampleSize, orientationMatrix, true)
        }
    }

    override fun isReady() = decoder != null && !decoder!!.isRecycled

    override fun recycle() {
        decoder!!.recycle()
    }
}

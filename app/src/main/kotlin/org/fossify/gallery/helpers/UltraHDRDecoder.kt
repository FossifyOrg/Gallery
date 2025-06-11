package org.fossify.gallery.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import java.io.InputStream

/**
 * Glide decoder for JPEG Ultra HDR images.
 * This decoder checks if the image contains a Gainmap and handles it appropriately.
 */
class UltraHDRDecoder(
    private val bitmapPool: BitmapPool
) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(source: InputStream, options: Options): Boolean {
        // Only handle on Android 14+ where Ultra HDR is supported
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap>? {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Decode with potential Ultra HDR support
            val bitmapOptions = BitmapFactory.Options().apply {
                // Set requested dimensions
                if (width > 0 && height > 0) {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeStream(source, null, this)

                    inSampleSize = calculateInSampleSize(this, width, height)
                    inJustDecodeBounds = false

                    // Reset stream for actual decoding
                    source.reset()
                }
            }

            val decodedBitmap = BitmapFactory.decodeStream(source, null, bitmapOptions)

            // Check if the bitmap has a Gainmap (Ultra HDR)
            decodedBitmap?.let {
                if (it.hasGainmap()) {
                    // The system will automatically apply the Gainmap
                    // when displayed in an HDR-capable context
                    it
                } else {
                    it
                }
            }
        } else {
            // Fallback for older Android versions
            BitmapFactory.decodeStream(source)
        }

        return if (bitmap != null) {
            BitmapResource(bitmap, bitmapPool)
        } else {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

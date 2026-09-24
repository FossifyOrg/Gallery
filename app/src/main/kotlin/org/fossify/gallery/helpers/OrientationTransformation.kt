package org.fossify.gallery.helpers

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE
import androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class OrientationTransformation(var orientation: Int) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {}

    companion object {
        fun getMatrix(orientation: Int): Matrix {
            val matrix = Matrix()
            when (orientation) {
                ORIENTATION_NORMAL -> {}
                ORIENTATION_ROTATE_270 -> {
                    matrix.postRotate(-90f)
                }
                ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1f, 1f)
                    matrix.postRotate(-90f)
                }
                ORIENTATION_ROTATE_180 -> {
                    matrix.postRotate(180f)
                }
                ORIENTATION_ROTATE_90 -> {
                    matrix.postRotate(90f)
                }
                ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1f, 1f)
                    matrix.postRotate(90f)
                }
                ORIENTATION_FLIP_HORIZONTAL -> {
                    matrix.postScale(-1f, 1f)
                }
                ORIENTATION_FLIP_VERTICAL -> {
                    matrix.postScale(1f, -1f)
                }
            }
            return matrix
        }

        fun whSwapped(orientation: Int) =
            orientation == ORIENTATION_ROTATE_90 || orientation == ORIENTATION_ROTATE_270 || orientation == ORIENTATION_TRANSPOSE || orientation == ORIENTATION_TRANSVERSE
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap? {
        val matrix = getMatrix(orientation)
        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
    }
}

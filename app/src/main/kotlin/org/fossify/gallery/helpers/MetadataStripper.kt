package org.fossify.gallery.helpers

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object MetadataStripper {
    private val SENSITIVE_TAGS = arrayOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_SUBJECT_LOCATION,
        ExifInterface.TAG_SUBJECT_AREA,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
    )

    private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "heic", "heif", "webp", "tiff", "tif", "png", "dng")

    fun isSupported(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val dotIndex = path.lastIndexOf('.')
        if (dotIndex < 0) return false
        val extension = path.substring(dotIndex + 1).lowercase(Locale.ROOT)
        return extension in SUPPORTED_EXTENSIONS
    }

    fun stripToCacheCopy(context: Context, sourcePath: String): String? {
        val source = File(sourcePath)
        if (!source.exists() || !source.canRead()) return null
        val cacheDir = File(context.cacheDir, "metadata-stripped").apply { mkdirs() }
        val copy = File(cacheDir, "${System.currentTimeMillis()}-${source.name}")
        FileInputStream(source).use { input ->
            FileOutputStream(copy).use { output -> input.copyTo(output) }
        }
        runCatching {
            val exif = ExifInterface(copy)
            for (tag in SENSITIVE_TAGS) {
                if (exif.getAttribute(tag) != null) exif.setAttribute(tag, null)
            }
            exif.saveAttributes()
        }
        return copy.absolutePath
    }
}

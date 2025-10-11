package org.fossify.gallery.extensions

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.InputStream

/**
 * A non-exhaustive list of all Exif attributes *excluding* dimension related attributes.
 */
val AllNonDimensionExifAttributes = listOf(
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_X_RESOLUTION,
    ExifInterface.TAG_Y_RESOLUTION,
    ExifInterface.TAG_RESOLUTION_UNIT,
    ExifInterface.TAG_GAMMA,
    ExifInterface.TAG_TRANSFER_FUNCTION,
    ExifInterface.TAG_WHITE_POINT,
    ExifInterface.TAG_PRIMARY_CHROMATICITIES,
    ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
    ExifInterface.TAG_Y_CB_CR_POSITIONING,
    ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
    ExifInterface.TAG_REFERENCE_BLACK_WHITE,
    ExifInterface.TAG_COLOR_SPACE,
    ExifInterface.TAG_FLASHPIX_VERSION,
    ExifInterface.TAG_EXIF_VERSION,
    ExifInterface.TAG_COMPONENTS_CONFIGURATION,
    ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_RELATED_SOUND_FILE,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_SPECTRAL_SENSITIVITY,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_OECF,
    ExifInterface.TAG_SENSITIVITY_TYPE,
    ExifInterface.TAG_STANDARD_OUTPUT_SENSITIVITY,
    ExifInterface.TAG_RECOMMENDED_EXPOSURE_INDEX,
    ExifInterface.TAG_ISO_SPEED,
    ExifInterface.TAG_ISO_SPEED_LATITUDE_YYY,
    ExifInterface.TAG_ISO_SPEED_LATITUDE_ZZZ,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_MAX_APERTURE_VALUE,
    ExifInterface.TAG_SUBJECT_DISTANCE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_LIGHT_SOURCE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_SUBJECT_AREA,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FLASH_ENERGY,
    ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
    ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
    ExifInterface.TAG_SUBJECT_LOCATION,
    ExifInterface.TAG_EXPOSURE_INDEX,
    ExifInterface.TAG_SENSING_METHOD,
    ExifInterface.TAG_FILE_SOURCE,
    ExifInterface.TAG_SCENE_TYPE,
    ExifInterface.TAG_CFA_PATTERN,
    ExifInterface.TAG_CUSTOM_RENDERED,
    ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_GAIN_CONTROL,
    ExifInterface.TAG_CONTRAST,
    ExifInterface.TAG_SATURATION,
    ExifInterface.TAG_SHARPNESS,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
    ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
    ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_CAMERA_OWNER_NAME,
    ExifInterface.TAG_BODY_SERIAL_NUMBER,
    ExifInterface.TAG_LENS_SPECIFICATION,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_LENS_SERIAL_NUMBER,
    ExifInterface.TAG_GPS_VERSION_ID,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_SATELLITES,
    ExifInterface.TAG_GPS_STATUS,
    ExifInterface.TAG_GPS_MEASURE_MODE,
    ExifInterface.TAG_GPS_DOP,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_MAP_DATUM,
    ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LATITUDE,
    ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE,
    ExifInterface.TAG_GPS_DEST_BEARING_REF,
    ExifInterface.TAG_GPS_DEST_BEARING,
    ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
    ExifInterface.TAG_GPS_DEST_DISTANCE,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_DIFFERENTIAL,
    ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
    ExifInterface.TAG_INTEROPERABILITY_INDEX,
    ExifInterface.TAG_XMP
)

fun ExifInterface.copyNonDimensionAttributesTo(destination: ExifInterface) {
    AllNonDimensionExifAttributes.forEach {
        val value = getAttribute(it)
        if (value != null) {
            destination.setAttribute(it, value)
        }
    }

    try {
        destination.saveAttributes()
    } catch (_: Exception) {
    }
}

fun Context.readExif(uri: Uri): ExifInterface? {
    var inputStream: InputStream? = null
    return try {
        inputStream = contentResolver.openInputStream(uri)
        ExifInterface(inputStream!!)
    } catch (_: Exception) {
        null
    } finally {
        inputStream?.close()
    }
}

fun Context.writeExif(exif: ExifInterface?, uri: Uri?) {
    if (exif == null || uri == null) return
    resolveUriScheme(
        uri = uri,
        onContentUri = {
            contentResolver.openFileDescriptor(it, "rw")?.use { pfd ->
                val destExif = ExifInterface(pfd.fileDescriptor)
                exif.copyNonDimensionAttributesTo(destExif)
            }
        },
        onPath = {
            val file = File(it)
            val destExif = ExifInterface(file.absolutePath)
            exif.copyNonDimensionAttributesTo(destExif)
        }
    )
}

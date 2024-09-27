package org.fossify.gallery.extensions

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
import androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED
import androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES
import androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
import androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
import androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
import androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
import androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED
import androidx.media3.common.Player
import org.fossify.gallery.R

fun Player.mute() {
    volume = 0f
}

fun Player.unmute() {
    volume = 1f
}

fun PlaybackException.getFriendlyMessage(context: Context): String {
    val resource = when (errorCode) {
        ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        ERROR_CODE_PARSING_MANIFEST_MALFORMED -> R.string.file_is_malformed_or_corrupted

        ERROR_CODE_DECODER_INIT_FAILED,
        ERROR_CODE_DECODING_FAILED,
        ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> R.string.media_exceeds_device_capabilities

        ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> R.string.unsupported_format

        else -> return localizedMessage ?: context.getString(R.string.failed_to_load_media)
    }

    return context.getString(resource)
}

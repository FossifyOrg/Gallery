package org.fossify.gallery.helpers

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.util.concurrent.CancellationException

/** Decodes QR payloads from ARGB pixels without Android or network dependencies. */
object QrCodeDecoder {
    /** Finds distinct payloads, including rotated codes and both foreground polarities. */
    fun decode(pixels: IntArray, width: Int, height: Int): List<String> {
        // Transparent image backgrounds are displayed on white for reliable luminance.
        val opaquePixels = IntArray(pixels.size) { index ->
            val color = pixels[index]
            val alpha = color ushr 24
            val red = ((color ushr 16 and 255) * alpha + 255 * (255 - alpha)) / 255
            val green = ((color ushr 8 and 255) * alpha + 255 * (255 - alpha)) / 255
            val blue = ((color and 255) * alpha + 255 * (255 - alpha)) / 255
            (red shl 16) or (green shl 8) or blue
        }
        val source = RGBLuminanceSource(width, height, opaquePixels)
        val reader = QRCodeMultiReader()
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)
        val payloads = linkedSetOf<String>()

        // QR finder patterns handle rotation; a second pass also finds light-on-dark codes.
        for (inverted in listOf(false, true)) {
            if (Thread.currentThread().isInterrupted) {
                throw CancellationException("QR decoding interrupted")
            }

            val luminance = if (inverted) source.invert() else source
            try {
                reader.decodeMultiple(BinaryBitmap(HybridBinarizer(luminance)), hints)
                    .forEach { payloads.add(it.text) }
            } catch (_: NotFoundException) {
                // No QR in this polarity is an ordinary empty result, not a load failure.
            }
        }

        return payloads.toList()
    }
}

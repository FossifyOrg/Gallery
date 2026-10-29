package org.fossify.gallery.helpers

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CancellationException

/** Exercises actual QR encoding and decoding without Android framework stubs. */
class QrCodeDecoderTest {
    private val size = 240
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()

    /** Preserves payload text rather than interpreting URLs or trimming whitespace. */
    @Test
    fun decodesGeneratedQrPayloadExactly() {
        val payload = "  https://example.org/private?key=abc\n"
        assertEquals(listOf(payload), QrCodeDecoder.decode(qr(payload), size, size))
    }

    /** Finds several separate QR symbols in one image. */
    @Test
    fun decodesMultipleQrCodes() {
        assertEquals(setOf("first", "second"), decodePair(qr("first"), qr("second")).toSet())
    }

    /** Finder-pattern orientation detection supports quarter turns. */
    @Test
    fun decodesEveryQuarterTurn() {
        var pixels = qr("rotated")
        repeat(3) {
            pixels = rotate(pixels)
            assertEquals(listOf("rotated"), QrCodeDecoder.decode(pixels, size, size))
        }
    }

    /** Inversion preserves decoding for light modules on a dark background. */
    @Test
    fun decodesInvertedQr() {
        val pixels = invert(rotate(qr("inverted")))
        assertEquals(listOf("inverted"), QrCodeDecoder.decode(pixels, size, size))
    }

    /** Transparent black backgrounds must not obscure opaque black QR modules. */
    @Test
    fun decodesTransparentBackground() {
        val pixels = qr("transparent").map { if (it == white) 0 else it }.toIntArray()
        assertEquals(listOf("transparent"), QrCodeDecoder.decode(pixels, size, size))
    }

    /** Featureless images in either polarity are ordinary no-match results. */
    @Test
    fun blankImagesHaveNoResults() {
        for (color in listOf(white, black)) {
            assertEquals(emptyList<String>(), QrCodeDecoder.decode(IntArray(size * size) { color }, size, size))
        }
    }

    /** Creates a real QR symbol with its encoder-provided quiet zone. */
    private fun qr(payload: String): IntArray {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        return IntArray(size * size) { index -> if (matrix[index % size, index / size]) black else white }
    }

    /** Places two symbols on a rectangular canvas separated by a quiet strip. */
    private fun decodePair(left: IntArray, right: IntArray): List<String> {
        val width = size * 2 + 40
        val pixels = IntArray(width * size) { white }
        repeat(size) { row ->
            left.copyInto(pixels, row * width, row * size, (row + 1) * size)
            right.copyInto(pixels, row * width + size + 40, row * size, (row + 1) * size)
        }
        return QrCodeDecoder.decode(pixels, width, size)
    }

    /** Rotates square pixel data clockwise without relying on Android bitmaps. */
    private fun rotate(pixels: IntArray): IntArray {
        return IntArray(pixels.size) { index ->
            val x = index % size
            val y = index / size
            pixels[(size - 1 - x) * size + y]
        }
    }

    /** Flips RGB channels while retaining opaque alpha. */
    private fun invert(pixels: IntArray): IntArray {
        return IntArray(pixels.size) { pixels[it] xor 0x00FFFFFF }
    }
}

@file:Suppress("LongMethod", "MagicNumber", "MaxLineLength", "TooManyFunctions")

package com.zomato.photofilters.imageprocessors

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin implementation of AndroidPhotoFilters' pixel operations.
 *
 * The upstream dependency ships libNativeImageProcessor.so, whose prebuilt arm64 ELF is only 4 KB
 * aligned. Keeping these operations in Kotlin removes that native library from Gallery's APK and
 * makes the editor compatible with 16 KB page-size devices.
 */
object ImageProcessor {
    fun applyCurves(
        rgb: IntArray?,
        red: IntArray?,
        green: IntArray?,
        blue: IntArray?,
        inputImage: Bitmap?
    ): Bitmap {
        val outputImage = requireNotNull(inputImage)
        val width = outputImage.width
        val height = outputImage.height
        var pixels = IntArray(width * height)
        outputImage.getPixels(pixels, 0, width, 0, 0, width, height)

        if (rgb != null) {
            pixels = applyRGBCurve(pixels, rgb)
        }

        if (!(red == null && green == null && blue == null)) {
            pixels = applyChannelCurves(pixels, red, green, blue)
        }

        try {
            outputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        } catch (_: IllegalStateException) {
        }
        return outputImage
    }

    fun doBrightness(value: Int, inputImage: Bitmap?): Bitmap {
        return processPixels(requireNotNull(inputImage)) { pixel ->
            val red = clampColor(((pixel shr RED_SHIFT) and CHANNEL_MASK) + value)
            val green = clampColor(((pixel shr GREEN_SHIFT) and CHANNEL_MASK) + value)
            val blue = clampColor((pixel and CHANNEL_MASK) + value)
            (pixel and ALPHA_MASK) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
        }
    }

    fun doContrast(value: Float, inputImage: Bitmap?): Bitmap {
        return processPixels(requireNotNull(inputImage)) { pixel ->
            val red = contrastChannel((pixel shr RED_SHIFT) and CHANNEL_MASK, value)
            val green = contrastChannel((pixel shr GREEN_SHIFT) and CHANNEL_MASK, value)
            val blue = contrastChannel(pixel and CHANNEL_MASK, value)
            (pixel and ALPHA_MASK) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
        }
    }

    fun doColorOverlay(
        depth: Int,
        red: Float,
        green: Float,
        blue: Float,
        inputImage: Bitmap?
    ): Bitmap {
        return processPixels(requireNotNull(inputImage)) { pixel ->
            val outRed = clampColor(((pixel shr RED_SHIFT) and CHANNEL_MASK) + (depth * red).toInt())
            val outGreen = clampColor(((pixel shr GREEN_SHIFT) and CHANNEL_MASK) + (depth * green).toInt())
            val outBlue = clampColor((pixel and CHANNEL_MASK) + (depth * blue).toInt())
            (pixel and ALPHA_MASK) or (outRed shl RED_SHIFT) or (outGreen shl GREEN_SHIFT) or outBlue
        }
    }

    fun doSaturation(inputImage: Bitmap?, level: Float): Bitmap {
        return processPixels(requireNotNull(inputImage)) { pixel ->
            saturatePixel(pixel, level)
        }
    }

    private fun applyRGBCurve(pixels: IntArray, rgb: IntArray): IntArray {
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val red = rgb[(pixel shr RED_SHIFT) and CHANNEL_MASK]
            val green = rgb[(pixel shr GREEN_SHIFT) and CHANNEL_MASK]
            val blue = rgb[pixel and CHANNEL_MASK]
            pixels[index] = (pixel and ALPHA_MASK) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue
        }
        return pixels
    }

    private fun applyChannelCurves(pixels: IntArray, red: IntArray?, green: IntArray?, blue: IntArray?): IntArray {
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val outRed = red?.get((pixel shr RED_SHIFT) and CHANNEL_MASK) ?: ((pixel shr RED_SHIFT) and CHANNEL_MASK)
            val outGreen = green?.get((pixel shr GREEN_SHIFT) and CHANNEL_MASK) ?: ((pixel shr GREEN_SHIFT) and CHANNEL_MASK)
            val outBlue = blue?.get(pixel and CHANNEL_MASK) ?: (pixel and CHANNEL_MASK)
            pixels[index] = (pixel and ALPHA_MASK) or (outRed shl RED_SHIFT) or (outGreen shl GREEN_SHIFT) or outBlue
        }
        return pixels
    }

    private inline fun processPixels(inputImage: Bitmap, transform: (Int) -> Int): Bitmap {
        val width = inputImage.width
        val height = inputImage.height
        val pixels = IntArray(width * height)
        inputImage.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            pixels[index] = transform(pixels[index])
        }
        inputImage.setPixels(pixels, 0, width, 0, 0, width, height)
        return inputImage
    }

    private fun contrastChannel(channel: Int, value: Float): Int {
        val contrasted = (((channel / COLOR_MAX_FLOAT - HALF) * value) + HALF) * COLOR_MAX_FLOAT
        return clampColor(contrasted.toInt())
    }

    private fun saturatePixel(pixel: Int, level: Float): Int {
        val redPercent = ((pixel shr RED_SHIFT) and CHANNEL_MASK) / COLOR_MAX_FLOAT
        val greenPercent = ((pixel shr GREEN_SHIFT) and CHANNEL_MASK) / COLOR_MAX_FLOAT
        val bluePercent = (pixel and CHANNEL_MASK) / COLOR_MAX_FLOAT

        val maxColor = max(redPercent, max(greenPercent, bluePercent))
        val minColor = min(redPercent, min(greenPercent, bluePercent))
        var luminance = ((maxColor + minColor) / 2f * PERCENT).toInt() / PERCENT_FLOAT

        if (maxColor == minColor) {
            val gray = clampColor((luminance * COLOR_MAX_FLOAT).toInt())
            return (pixel and ALPHA_MASK) or (gray shl RED_SHIFT) or (gray shl GREEN_SHIFT) or gray
        }

        var saturation = if (luminance < HALF) {
            (maxColor - minColor) / (maxColor + minColor)
        } else {
            (maxColor - minColor) / (TWO - maxColor - minColor)
        }

        var hue = when (maxColor) {
            redPercent -> (greenPercent - bluePercent) / (maxColor - minColor)
            greenPercent -> TWO + (bluePercent - redPercent) / (maxColor - minColor)
            else -> FOUR + (redPercent - greenPercent) / (maxColor - minColor)
        } * HUE_DEGREES
        if (hue < 0f) {
            hue += FULL_CIRCLE_DEGREES
        }

        saturation = ((saturation * PERCENT).toInt() * level).coerceIn(0f, PERCENT_FLOAT) / PERCENT_FLOAT
        hue /= FULL_CIRCLE_DEGREES

        val temp1 = if (luminance < HALF) {
            luminance * (1f + saturation)
        } else {
            luminance + saturation - luminance * saturation
        }
        val temp2 = TWO * luminance - temp1

        val outRed = hslComponentToColor(temp1, temp2, normalizeHue(hue + ONE_THIRD))
        val outGreen = hslComponentToColor(temp1, temp2, hue)
        val outBlue = hslComponentToColor(temp1, temp2, normalizeHue(hue - ONE_THIRD))
        return (pixel and ALPHA_MASK) or (outRed shl RED_SHIFT) or (outGreen shl GREEN_SHIFT) or outBlue
    }

    private fun hslComponentToColor(temp1: Float, temp2: Float, temp3: Float): Int {
        val component = when {
            temp3 * SIX < 1f -> temp2 + (temp1 - temp2) * SIX * temp3
            temp3 * TWO < 1f -> temp1
            temp3 * THREE < TWO -> temp2 + (temp1 - temp2) * (TWO_THIRDS - temp3) * SIX
            else -> temp2
        }
        return clampColor(((component * PERCENT).toInt() / PERCENT_FLOAT * COLOR_MAX_FLOAT).toInt())
    }

    private fun normalizeHue(hue: Float): Float {
        return when {
            hue > 1f -> hue - 1f
            hue < 0f -> hue + 1f
            else -> hue
        }
    }

    private fun clampColor(value: Int): Int = value.coerceIn(0, CHANNEL_MASK)

    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val CHANNEL_MASK = 0xFF
    private const val ALPHA_MASK = -0x1000000
    private const val COLOR_MAX_FLOAT = 255f
    private const val HALF = 0.5f
    private const val PERCENT = 100
    private const val PERCENT_FLOAT = 100f
    private const val HUE_DEGREES = 60f
    private const val FULL_CIRCLE_DEGREES = 360f
    private const val ONE_THIRD = 0.33333f
    private const val TWO_THIRDS = 0.66666f
    private const val TWO = 2f
    private const val THREE = 3f
    private const val FOUR = 4f
    private const val SIX = 6f
}

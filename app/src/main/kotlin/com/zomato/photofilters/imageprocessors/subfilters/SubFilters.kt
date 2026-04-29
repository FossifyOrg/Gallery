@file:Suppress("LongParameterList", "MagicNumber", "UnusedPrivateProperty")

package com.zomato.photofilters.imageprocessors.subfilters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.zomato.photofilters.geometry.BezierSpline
import com.zomato.photofilters.geometry.Point
import com.zomato.photofilters.imageprocessors.ImageProcessor
import com.zomato.photofilters.imageprocessors.SubFilter
import kotlin.math.hypot

class BrightnessSubFilter(private var brightness: Int) : SubFilter {
    override fun process(inputImage: Bitmap?): Bitmap {
        return ImageProcessor.doBrightness(brightness, inputImage)
    }

    override var tag: Any = ""

    fun changeBrightness(value: Int) {
        brightness += value
    }
}

class ColorOverlaySubFilter(
    private val colorOverlayDepth: Int,
    private val colorOverlayRed: Float,
    private val colorOverlayGreen: Float,
    private val colorOverlayBlue: Float
) : SubFilter {
    override fun process(inputImage: Bitmap?): Bitmap {
        return ImageProcessor.doColorOverlay(
            colorOverlayDepth,
            colorOverlayRed,
            colorOverlayGreen,
            colorOverlayBlue,
            inputImage
        )
    }

    override var tag: Any = ""
}

class ContrastSubFilter(var contrast: Float) : SubFilter {
    override fun process(inputImage: Bitmap?): Bitmap {
        return ImageProcessor.doContrast(contrast, inputImage)
    }

    override var tag: Any = ""

    fun changeContrast(value: Float) {
        contrast += value
    }
}

class SaturationSubFilter(var saturation: Float) : SubFilter {
    override fun process(inputImage: Bitmap?): Bitmap {
        return ImageProcessor.doSaturation(inputImage, saturation)
    }

    override var tag: Any = ""

    fun setLevel(level: Float) {
        saturation = level
    }
}

class ToneCurveSubFilter(
    rgbKnots: Array<Point?>?,
    redKnots: Array<Point?>?,
    greenKnots: Array<Point?>?,
    blueKnots: Array<Point?>?
) : SubFilter {
    private var rgbKnots: Array<Point?>? = rgbKnots ?: straightKnots()
    private var redKnots: Array<Point?>? = redKnots ?: straightKnots()
    private var greenKnots: Array<Point?>? = greenKnots ?: straightKnots()
    private var blueKnots: Array<Point?>? = blueKnots ?: straightKnots()
    private var rgb: IntArray? = null
    private var red: IntArray? = null
    private var green: IntArray? = null
    private var blue: IntArray? = null

    override fun process(inputImage: Bitmap?): Bitmap {
        rgbKnots = sortPointsOnXAxis(rgbKnots)
        redKnots = sortPointsOnXAxis(redKnots)
        greenKnots = sortPointsOnXAxis(greenKnots)
        blueKnots = sortPointsOnXAxis(blueKnots)

        if (rgb == null) rgb = BezierSpline.curveGenerator(rgbKnots)
        if (red == null) red = BezierSpline.curveGenerator(redKnots)
        if (green == null) green = BezierSpline.curveGenerator(greenKnots)
        if (blue == null) blue = BezierSpline.curveGenerator(blueKnots)

        return ImageProcessor.applyCurves(rgb, red, green, blue, inputImage)
    }

    fun sortPointsOnXAxis(points: Array<Point?>?): Array<Point?>? {
        points ?: return null
        repeat((points.size - 2).coerceAtLeast(0)) {
            for (index in 0..points.size - 2) {
                if (points[index]!!.x > points[index + 1]!!.x) {
                    val temp = points[index]!!.x
                    points[index]!!.x = points[index + 1]!!.x
                    points[index + 1]!!.x = temp
                }
            }
        }
        return points
    }

    override var tag: Any = ""

    companion object {
        private fun straightKnots() = arrayOf<Point?>(Point(0f, 0f), Point(255f, 255f))
    }
}

class VignetteSubFilter(@Suppress("UNUSED_PARAMETER") context: Context, private var alpha: Int) : SubFilter {
    override fun process(inputImage: Bitmap?): Bitmap {
        val bitmap = requireNotNull(inputImage)
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = hypot(centerX, centerY)
        val vignette = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.BLACK),
            floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = alpha.coerceIn(0, 255)
            shader = vignette
        }
        Canvas(bitmap).drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return bitmap
    }

    override var tag: Any = ""

    fun setAlpha(alpha: Int) {
        this.alpha = alpha
    }

    fun changeAlpha(value: Int) {
        alpha = (alpha + value).coerceIn(0, 255)
    }
}

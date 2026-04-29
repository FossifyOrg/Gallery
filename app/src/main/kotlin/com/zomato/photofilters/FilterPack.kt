@file:Suppress(
    "MagicNumber",
    "LongMethod",
    "LongParameterList",
    "ReturnCount",
    "TooManyFunctions",
    "UnusedParameter"
)

package com.zomato.photofilters

import android.content.Context
import com.zomato.photofilters.geometry.Point
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ColorOverlaySubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ToneCurveSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.VignetteSubFilter

object FilterPack {
    /***
     * the filter pack,
     * @param context
     * @return list of filters
     */
    fun getFilterPack(context: Context): List<Filter> {
        val filters: MutableList<Filter> = ArrayList()
        filters.add(getAweStruckVibeFilter(context))
        filters.add(getClarendon(context))
        filters.add(getOldManFilter(context))
        filters.add(getMarsFilter(context))
        filters.add(getRiseFilter(context))
        filters.add(getAprilFilter(context))
        filters.add(getAmazonFilter(context))
        filters.add(getStarLitFilter(context))
        filters.add(getNightWhisperFilter(context))
        filters.add(getLimeStutterFilter(context))
        filters.add(getHaanFilter(context))
        filters.add(getBlueMessFilter(context))
        filters.add(getAdeleFilter(context))
        filters.add(getCruzFilter(context))
        filters.add(getMetropolis(context))
        filters.add(getAudreyFilter(context))
        return filters
    }

    fun getStarLitFilter(context: Context): Filter {
        val rgbKnots = arrayOfNulls<Point>(8)
        rgbKnots[0] = Point(0f, 0f)
        rgbKnots[1] = Point(34f, 6f)
        rgbKnots[2] = Point(69f, 23f)
        rgbKnots[3] = Point(100f, 58f)
        rgbKnots[4] = Point(150f, 154f)
        rgbKnots[5] = Point(176f, 196f)
        rgbKnots[6] = Point(207f, 233f)
        rgbKnots[7] = Point(255f, 255f)
        val filter = Filter()
        filter.name = "Starlit"
        filter.addSubFilter(ToneCurveSubFilter(rgbKnots, null, null, null))
        return filter
    }

    fun getBlueMessFilter(context: Context): Filter {
        val redKnots = arrayOfNulls<Point>(8)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(86f, 34f)
        redKnots[2] = Point(117f, 41f)
        redKnots[3] = Point(146f, 80f)
        redKnots[4] = Point(170f, 151f)
        redKnots[5] = Point(200f, 214f)
        redKnots[6] = Point(225f, 242f)
        redKnots[7] = Point(255f, 255f)
        val filter = Filter()
        filter.name = "BlueMess"
        filter.addSubFilter(ToneCurveSubFilter(null, redKnots, null, null))
        filter.addSubFilter(BrightnessSubFilter(30))
        filter.addSubFilter(ContrastSubFilter(1f))
        return filter
    }

    fun getAweStruckVibeFilter(context: Context): Filter {
        val rgbKnots = arrayOfNulls<Point>(5)
        rgbKnots[0] = Point(0f, 0f)
        rgbKnots[1] = Point(80f, 43f)
        rgbKnots[2] = Point(149f, 102f)
        rgbKnots[3] = Point(201f, 173f)
        rgbKnots[4] = Point(255f, 255f)

        val redKnots = arrayOfNulls<Point>(5)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(125f, 147f)
        redKnots[2] = Point(177f, 199f)
        redKnots[3] = Point(213f, 228f)
        redKnots[4] = Point(255f, 255f)


        val greenKnots = arrayOfNulls<Point>(6)
        greenKnots[0] = Point(0f, 0f)
        greenKnots[1] = Point(57f, 76f)
        greenKnots[2] = Point(103f, 130f)
        greenKnots[3] = Point(167f, 192f)
        greenKnots[4] = Point(211f, 229f)
        greenKnots[5] = Point(255f, 255f)


        val blueKnots = arrayOfNulls<Point>(7)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(38f, 62f)
        blueKnots[2] = Point(75f, 112f)
        blueKnots[3] = Point(116f, 158f)
        blueKnots[4] = Point(171f, 204f)
        blueKnots[5] = Point(212f, 233f)
        blueKnots[6] = Point(255f, 255f)

        val filter = Filter()
        filter.name = "Struck"
        filter.addSubFilter(ToneCurveSubFilter(rgbKnots, redKnots, greenKnots, blueKnots))
        return filter
    }

    fun getLimeStutterFilter(context: Context): Filter {
        val blueKnots = arrayOfNulls<Point>(3)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(165f, 114f)
        blueKnots[2] = Point(255f, 255f)
        val filter = Filter()
        filter.name = "Lime"
        filter.addSubFilter(ToneCurveSubFilter(null, null, null, blueKnots))
        return filter
    }

    fun getNightWhisperFilter(context: Context): Filter {
        val rgbKnots = arrayOfNulls<Point>(3)
        rgbKnots[0] = Point(0f, 0f)
        rgbKnots[1] = Point(174f, 109f)
        rgbKnots[2] = Point(255f, 255f)

        val redKnots = arrayOfNulls<Point>(4)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(70f, 114f)
        redKnots[2] = Point(157f, 145f)
        redKnots[3] = Point(255f, 255f)

        val greenKnots = arrayOfNulls<Point>(3)
        greenKnots[0] = Point(0f, 0f)
        greenKnots[1] = Point(109f, 138f)
        greenKnots[2] = Point(255f, 255f)

        val blueKnots = arrayOfNulls<Point>(3)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(113f, 152f)
        blueKnots[2] = Point(255f, 255f)

        val filter = Filter()
        filter.name = "Whisper"
        filter.addSubFilter(ContrastSubFilter(1.5f))
        filter.addSubFilter(ToneCurveSubFilter(rgbKnots, redKnots, greenKnots, blueKnots))
        return filter
    }

    fun getAmazonFilter(context: Context): Filter {
        val blueKnots = arrayOfNulls<Point>(6)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(11f, 40f)
        blueKnots[2] = Point(36f, 99f)
        blueKnots[3] = Point(86f, 151f)
        blueKnots[4] = Point(167f, 209f)
        blueKnots[5] = Point(255f, 255f)
        val filter = Filter("Amazon")
        filter.addSubFilter(ContrastSubFilter(1.2f))
        filter.addSubFilter(ToneCurveSubFilter(null, null, null, blueKnots))
        return filter
    }

    fun getAdeleFilter(context: Context): Filter {
        val filter = Filter("Adele")
        filter.addSubFilter(SaturationSubFilter(-100f))
        return filter
    }

    fun getCruzFilter(context: Context): Filter {
        val filter = Filter("Cruz")
        filter.addSubFilter(SaturationSubFilter(-100f))
        filter.addSubFilter(ContrastSubFilter(1.3f))
        filter.addSubFilter(BrightnessSubFilter(20))
        return filter
    }

    fun getMetropolis(context: Context): Filter {
        val filter = Filter("Metropolis")
        filter.addSubFilter(SaturationSubFilter(-1f))
        filter.addSubFilter(ContrastSubFilter(1.7f))
        filter.addSubFilter(BrightnessSubFilter(70))
        return filter
    }

    fun getAudreyFilter(context: Context): Filter {
        val filter = Filter("Audrey")
        val redKnots = arrayOfNulls<Point>(3)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(124f, 138f)
        redKnots[2] = Point(255f, 255f)

        filter.addSubFilter(SaturationSubFilter(-100f))
        filter.addSubFilter(ContrastSubFilter(1.3f))
        filter.addSubFilter(BrightnessSubFilter(20))
        filter.addSubFilter(ToneCurveSubFilter(null, redKnots, null, null))
        return filter
    }

    fun getRiseFilter(context: Context): Filter {
        val blueKnots = arrayOfNulls<Point>(4)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(39f, 70f)
        blueKnots[2] = Point(150f, 200f)
        blueKnots[3] = Point(255f, 255f)

        val redKnots = arrayOfNulls<Point>(4)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(45f, 64f)
        redKnots[2] = Point(170f, 190f)
        redKnots[3] = Point(255f, 255f)

        val filter = Filter("Rise")
        filter.addSubFilter(ContrastSubFilter(1.9f))
        filter.addSubFilter(BrightnessSubFilter(60))
        filter.addSubFilter(VignetteSubFilter(context, 200))
        filter.addSubFilter(ToneCurveSubFilter(null, redKnots, null, blueKnots))
        return filter
    }

    fun getMarsFilter(context: Context): Filter {
        val filter = Filter("Mars")
        filter.addSubFilter(ContrastSubFilter(1.5f))
        filter.addSubFilter(BrightnessSubFilter(10))
        return filter
    }

    fun getAprilFilter(context: Context): Filter {
        val blueKnots = arrayOfNulls<Point>(4)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(39f, 70f)
        blueKnots[2] = Point(150f, 200f)
        blueKnots[3] = Point(255f, 255f)

        val redKnots = arrayOfNulls<Point>(4)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(45f, 64f)
        redKnots[2] = Point(170f, 190f)
        redKnots[3] = Point(255f, 255f)

        val filter = Filter("April")
        filter.addSubFilter(ContrastSubFilter(1.5f))
        filter.addSubFilter(BrightnessSubFilter(5))
        filter.addSubFilter(VignetteSubFilter(context, 150))
        filter.addSubFilter(ToneCurveSubFilter(null, redKnots, null, blueKnots))
        return filter
    }

    fun getHaanFilter(context: Context): Filter {
        val greenKnots = arrayOfNulls<Point>(3)
        greenKnots[0] = Point(0f, 0f)
        greenKnots[1] = Point(113f, 142f)
        greenKnots[2] = Point(255f, 255f)

        val filter = Filter("Haan")
        filter.addSubFilter(ContrastSubFilter(1.3f))
        filter.addSubFilter(BrightnessSubFilter(60))
        filter.addSubFilter(VignetteSubFilter(context, 200))
        filter.addSubFilter(ToneCurveSubFilter(null, null, greenKnots, null))
        return filter
    }

    fun getOldManFilter(context: Context): Filter {
        val filter = Filter("OldMan")
        filter.addSubFilter(BrightnessSubFilter(30))
        filter.addSubFilter(SaturationSubFilter(0.8f))
        filter.addSubFilter(ContrastSubFilter(1.3f))
        filter.addSubFilter(VignetteSubFilter(context, 100))
        filter.addSubFilter(ColorOverlaySubFilter(100, .2f, .2f, .1f))
        return filter
    }

    fun getClarendon(context: Context): Filter {
        val redKnots = arrayOfNulls<Point>(4)
        redKnots[0] = Point(0f, 0f)
        redKnots[1] = Point(56f, 68f)
        redKnots[2] = Point(196f, 206f)
        redKnots[3] = Point(255f, 255f)


        val greenKnots = arrayOfNulls<Point>(4)
        greenKnots[0] = Point(0f, 0f)
        greenKnots[1] = Point(46f, 77f)
        greenKnots[2] = Point(160f, 200f)
        greenKnots[3] = Point(255f, 255f)


        val blueKnots = arrayOfNulls<Point>(4)
        blueKnots[0] = Point(0f, 0f)
        blueKnots[1] = Point(33f, 86f)
        blueKnots[2] = Point(126f, 220f)
        blueKnots[3] = Point(255f, 255f)

        val filter = Filter("Clarendon")
        filter.addSubFilter(ContrastSubFilter(1.5f))
        filter.addSubFilter(BrightnessSubFilter(-10))
        filter.addSubFilter(ToneCurveSubFilter(null, redKnots, greenKnots, blueKnots))
        return filter
    }
}

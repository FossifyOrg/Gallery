@file:Suppress("MagicNumber", "LongMethod", "ReturnCount", "LongParameterList")

package com.zomato.photofilters.imageprocessors

import android.graphics.Bitmap

interface SubFilter {
    fun process(inputImage: Bitmap?): Bitmap?

    var tag: Any
}

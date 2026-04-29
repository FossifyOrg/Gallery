@file:Suppress(
    "ExplicitGarbageCollectionCall",
    "LongMethod",
    "LongParameterList",
    "MagicNumber",
    "NestedBlockDepth",
    "ReturnCount",
    "SwallowedException"
)

package com.zomato.photofilters.imageprocessors

import android.graphics.Bitmap
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ColorOverlaySubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ToneCurveSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.VignetteSubFilter

/**
 * Represents an image filter containing subfilters that are applied in insertion order.
 */
class Filter {
    private val subFilters: MutableList<SubFilter> = ArrayList()
    var name: String? = null

    constructor(filter: Filter) {
        subFilters.addAll(filter.subFilters)
        name = filter.name
    }

    constructor()

    constructor(name: String?) {
        this.name = name
    }

    /**
     * Adds a Subfilter to the Main Filter.
     *
     * @see BrightnessSubFilter
     * @see ColorOverlaySubFilter
     * @see ContrastSubFilter
     * @see ToneCurveSubFilter
     * @see VignetteSubFilter
     * @see SaturationSubFilter
     */
    fun addSubFilter(subFilter: SubFilter) {
        subFilters.add(subFilter)
    }

    /**
     * Adds all [SubFilter]s from the List to the Main Filter.
     */
    fun addSubFilters(subFilterList: List<SubFilter>?) {
        subFilterList?.let(subFilters::addAll)
    }

    /**
     * Get a new list of currently applied subfilters.
     */
    fun getSubFilters(): List<SubFilter> {
        if (subFilters.isEmpty()) return ArrayList(0)
        return ArrayList(subFilters)
    }

    /**
     * Clears all the subfilters from the Parent Filter.
     */
    fun clearSubFilters() {
        subFilters.clear()
    }

    /**
     * Removes the subfilter containing Tag from the Parent Filter.
     */
    fun removeSubFilterWithTag(tag: String) {
        val iterator = subFilters.iterator()
        while (iterator.hasNext()) {
            val subFilter = iterator.next()
            if (subFilter.tag == tag) {
                iterator.remove()
            }
        }
    }

    /**
     * Returns The filter containing Tag.
     */
    fun getSubFilterByTag(tag: String): SubFilter? {
        for (subFilter in subFilters) {
            if (subFilter.tag == tag) {
                return subFilter
            }
        }
        return null
    }

    /**
     * Give the output Bitmap by applying the defined filter.
     */
    fun processFilter(inputImage: Bitmap?): Bitmap? {
        var outputImage = inputImage
        if (outputImage != null) {
            for (subFilter in subFilters) {
                try {
                    outputImage = subFilter.process(outputImage)
                } catch (oe: OutOfMemoryError) {
                    System.gc()
                    try {
                        outputImage = subFilter.process(outputImage)
                    } catch (ignored: OutOfMemoryError) {
                    }
                }
            }
        }

        return outputImage
    }
}

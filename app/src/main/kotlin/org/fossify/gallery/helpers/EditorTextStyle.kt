package org.fossify.gallery.helpers

import android.graphics.Paint
import android.graphics.Typeface
import android.util.Xml
import android.widget.TextView
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Locale

private const val SYSTEM_FONTS_XML = "/system/etc/fonts.xml"
private const val FONT_FAMILY_TAG = "family"
private const val FONT_ALIAS_TAG = "alias"
private const val FONT_NAME_ATTRIBUTE = "name"

private val PREFERRED_FONT_FAMILIES = listOf("sans-serif", "serif", "monospace")
private val FALLBACK_FONT_FAMILIES = listOf("sans-serif", "serif", "monospace", "cursive", "casual")

fun loadSystemFontFamilies(): List<String> {
    val names = runCatching { readSystemFontNames() }.getOrNull().orEmpty()
    return if (names.isEmpty()) {
        FALLBACK_FONT_FAMILIES
    } else {
        orderFontFamilies(names)
    }
}

fun editorFontDisplayName(family: String): String = family.replace('-', ' ')

fun TextView.applyEditorTextStyle(style: Int, fontFamily: String) {
    val typefaceStyle = style and (TEXT_STYLE_BOLD or TEXT_STYLE_ITALIC)
    typeface = Typeface.create(fontFamily, typefaceStyle)
    var flags = paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv() and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    if ((style and TEXT_STYLE_UNDERLINE) != 0) {
        flags = flags or Paint.UNDERLINE_TEXT_FLAG
    }
    if ((style and TEXT_STYLE_STRIKE_THROUGH) != 0) {
        flags = flags or Paint.STRIKE_THRU_TEXT_FLAG
    }
    paintFlags = flags
}

private fun readSystemFontNames(): Set<String> {
    val names = linkedSetOf<String>()
    File(SYSTEM_FONTS_XML).inputStream().use { input ->
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name
            if (event == XmlPullParser.START_TAG && tag != null && tag.isFontFamilyTag()) {
                val name = parser.attributeValue(FONT_NAME_ATTRIBUTE)
                if (!name.isNullOrBlank()) {
                    names.add(name)
                }
            }
            event = parser.next()
        }
    }
    return names
}

private fun orderFontFamilies(names: Set<String>): List<String> {
    val preferred = PREFERRED_FONT_FAMILIES.filter { it in names }
    val rest = names
        .filter { it !in PREFERRED_FONT_FAMILIES }
        .sortedBy { it.lowercase(Locale.ROOT) }
    return preferred + rest
}

private fun String.isFontFamilyTag(): Boolean {
    return this == FONT_FAMILY_TAG || this == FONT_ALIAS_TAG
}

private fun XmlPullParser.attributeValue(attribute: String): String? {
    for (index in 0 until attributeCount) {
        if (getAttributeName(index) == attribute) {
            return getAttributeValue(index)
        }
    }
    return null
}

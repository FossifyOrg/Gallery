package org.fossify.gallery.dialogs

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import kotlin.math.max
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.gallery.R
import org.fossify.gallery.databinding.DialogTextStyleBinding
import org.fossify.gallery.extensions.showContentDescriptionOnLongClick
import org.fossify.gallery.helpers.DEFAULT_EDITOR_TEXT_FONT
import org.fossify.gallery.helpers.TEXT_STYLE_BOLD
import org.fossify.gallery.helpers.TEXT_STYLE_ITALIC
import org.fossify.gallery.helpers.TEXT_STYLE_NO_SHADOW
import org.fossify.gallery.helpers.TEXT_STYLE_STRIKE_THROUGH
import org.fossify.gallery.helpers.TEXT_STYLE_UNDERLINE
import org.fossify.gallery.helpers.applyEditorTextStyle
import org.fossify.gallery.helpers.editorFontDisplayName
import org.fossify.gallery.helpers.loadSystemFontFamilies

class TextStyleDialog(
    val activity: BaseSimpleActivity,
    style: Int,
    fontFamily: String,
    private val callback: (style: Int, fontFamily: String) -> Unit,
) {
    private val binding = DialogTextStyleBinding.inflate(activity.layoutInflater)
    private val choices = fontChoices(fontFamily)
    private val fontAdapter = FontFamilyAdapter(activity, choices)
    private var selectedFamily = choices.firstOrNull { it.family == fontFamily }?.family
        ?: choices.firstOrNull()?.family
        ?: DEFAULT_EDITOR_TEXT_FONT

    init {
        setupStyleIcons(style)
        setupFontSelector()
        refreshSample()

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok) { _, _ ->
                callback(currentStyle(), currentFont())
            }
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.text_style)
            }
    }

    private fun setupStyleIcons(style: Int) {
        bindToggle(binding.textStyleBold, (style and TEXT_STYLE_BOLD) != 0)
        bindToggle(binding.textStyleItalic, (style and TEXT_STYLE_ITALIC) != 0)
        bindToggle(binding.textStyleUnderline, (style and TEXT_STYLE_UNDERLINE) != 0)
        bindToggle(binding.textStyleStrike, (style and TEXT_STYLE_STRIKE_THROUGH) != 0)
        bindToggle(binding.textStyleShadow, (style and TEXT_STYLE_NO_SHADOW) == 0)
    }

    private fun bindToggle(view: ImageView, selected: Boolean) {
        view.isSelected = selected
        tintStyleIcon(view)
        view.setOnClickListener {
            view.isSelected = !view.isSelected
            tintStyleIcon(view)
            refreshSample()
        }
        view.showContentDescriptionOnLongClick()
    }

    private fun tintStyleIcon(view: ImageView) {
        val color = if (view.isSelected) {
            activity.getProperPrimaryColor()
        } else {
            activity.getProperTextColor()
        }
        view.applyColorFilter(color)
    }

    private fun setupFontSelector() {
        val selected = choices.firstOrNull { it.family == selectedFamily }
        binding.textStyleFont.setAdapter(fontAdapter)
        binding.textStyleFont.setText(selected?.label.orEmpty(), false)
        binding.textStyleFont.setOnItemClickListener { _, _, position, _ ->
            val choice = fontAdapter.getItem(position) ?: return@setOnItemClickListener
            selectedFamily = choice.family
            refreshSample()
        }
        keepFontFieldPadding()
    }

    private fun keepFontFieldPadding() {
        val start = activity.resources.getDimensionPixelSize(org.fossify.commons.R.dimen.activity_margin)
        val minEnd = activity.resources.getDimensionPixelSize(R.dimen.bottom_editor_color_picker_size)
        val field = binding.textStyleFont
        field.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            private var applied = false

            override fun onLayoutChange(
                view: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                if (applied) {
                    return
                }
                val end = max(view.paddingEnd, minEnd)
                if (view.paddingStart >= start && view.paddingEnd >= minEnd) {
                    return
                }
                applied = true
                view.setPaddingRelative(start, view.paddingTop, end, view.paddingBottom)
            }
        })
    }

    private fun refreshSample() {
        binding.textStyleSample.applyEditorTextStyle(currentStyle(), currentFont())
    }

    private fun currentStyle(): Int {
        var style = 0
        if (binding.textStyleBold.isSelected) {
            style = style or TEXT_STYLE_BOLD
        }
        if (binding.textStyleItalic.isSelected) {
            style = style or TEXT_STYLE_ITALIC
        }
        if (binding.textStyleUnderline.isSelected) {
            style = style or TEXT_STYLE_UNDERLINE
        }
        if (binding.textStyleStrike.isSelected) {
            style = style or TEXT_STYLE_STRIKE_THROUGH
        }
        if (!binding.textStyleShadow.isSelected) {
            style = style or TEXT_STYLE_NO_SHADOW
        }
        return style
    }

    private fun currentFont(): String = selectedFamily

    private fun fontChoices(currentFamily: String): List<FontChoice> {
        val families = loadSystemFontFamilies().toMutableList()
        if (currentFamily.isNotBlank() && currentFamily !in families) {
            families.add(currentFamily)
        }
        return families.map { FontChoice(it, editorFontDisplayName(it)) }
    }
}

private data class FontChoice(val family: String, val label: String) {
    override fun toString(): String = label
}

private class FontFamilyAdapter(
    context: Context,
    families: List<FontChoice>,
) : ArrayAdapter<FontChoice>(context, android.R.layout.simple_list_item_1, families) {
    private val all = families.toList()

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults().apply {
                values = all
                count = all.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        bindFont(view, getItem(position))
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        bindFont(view, getItem(position))
        return view
    }

    private fun bindFont(view: View, choice: FontChoice?) {
        val label = view as? TextView ?: return
        if (choice == null) {
            return
        }
        label.text = choice.label
        label.typeface = Typeface.create(choice.family, Typeface.NORMAL)
    }
}

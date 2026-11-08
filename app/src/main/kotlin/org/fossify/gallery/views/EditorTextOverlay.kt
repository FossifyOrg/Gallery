package org.fossify.gallery.views

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.method.ArrowKeyMovementMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.fossify.commons.R as CommonsR
import org.fossify.gallery.R
import org.fossify.gallery.helpers.applyEditorTextStyle
import kotlin.math.hypot
import kotlin.math.min

data class EditorTextAppearance(
    val sizeSp: Float,
    val color: Int,
    val style: Int,
    val fontFamily: String,
)

class EditorTextOverlay(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {
    var gesturesEnabled = false
    var onSelectionChanged: ((EditorTextAppearance?) -> Unit)? = null
    var onSelectedSizeChanged: ((Float) -> Unit)? = null

    private val captions = mutableListOf<TextCaption>()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private var selected: TextCaption? = null
    private var dragging = false
    private var moved = false
    private var pinching = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var pinchStartSpan = 1f
    private var pinchStartSize = MIN_TEXT_SP
    private var imeBottom = 0
    private val longPressRunnable = Runnable {
        if (!moved) {
            selected?.startEditing()
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        isClickable = true
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            selected?.updateLift(imeBottom, rootView.height)
            insets
        }
    }

    fun hasContent(): Boolean = captions.isNotEmpty()

    fun addText(appearance: EditorTextAppearance) {
        val caption = TextCaption(context, appearance)
        val maxWidth = (width * TEXT_WIDTH_FRACTION).toInt()
        if (maxWidth > 0) {
            caption.view.maxWidth = maxWidth
        }
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        addView(caption.view, params)
        captions.add(caption)
        select(caption)
        caption.startEditing()
    }

    fun applyToSelected(appearance: EditorTextAppearance) {
        selected?.update(appearance)
    }

    fun finishTool() {
        clearSelection(notify = true)
        val blanks = captions.filter { it.view.text.isNullOrBlank() }
        blanks.forEach { removeView(it.view) }
        captions.removeAll(blanks.toSet())
    }

    internal fun captureStamps(imageView: ImageView): List<EditorTextStamp> {
        return captions.mapNotNull { it.toStamp(this, imageView) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val maxWidth = (w * TEXT_WIDTH_FRACTION).toInt()
        if (maxWidth <= 0) {
            return
        }
        captions.forEach { it.view.maxWidth = maxWidth }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!gesturesEnabled || selected == null) {
            return gesturesEnabled && event.actionMasked == MotionEvent.ACTION_DOWN
        }
        if (event.pointerCount >= 2) {
            return true
        }
        val caption = selected
        val editingThis = caption != null && caption.editing && caption.contains(event.x, event.y)
        if (editingThis) {
            return false
        }
        return event.actionMasked == MotionEvent.ACTION_DOWN || dragging
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!gesturesEnabled) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pinching = false
                moved = false
                dragging = false
                val hit = captions.lastOrNull { it.contains(event.x, event.y) }
                if (hit == null) {
                    clearSelection(notify = true)
                    performClick()
                    return true
                }
                select(hit)
                downRawX = event.rawX
                downRawY = event.rawY
                postDelayed(longPressRunnable, longPressTimeout)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val caption = selected ?: return true
                if (event.pointerCount < 2) {
                    return true
                }
                pinching = true
                dragging = false
                moved = true
                removeCallbacks(longPressRunnable)
                pinchStartSpan = event.fingerSpan()
                pinchStartSize = caption.appearance.sizeSp
            }
            MotionEvent.ACTION_MOVE -> {
                val caption = selected ?: return true
                if (pinching && event.pointerCount >= 2) {
                    val size = pinchStartSize * event.fingerSpan() / pinchStartSpan
                    val nextSize = size.coerceIn(MIN_TEXT_SP, MAX_TEXT_SP)
                    caption.update(caption.appearance.copy(sizeSp = nextSize))
                    onSelectedSizeChanged?.invoke(nextSize)
                    return true
                }
                val deltaX = event.rawX - downRawX
                val deltaY = event.rawY - downRawY
                if (!dragging && deltaX * deltaX + deltaY * deltaY > touchSlop * touchSlop) {
                    dragging = true
                    moved = true
                    removeCallbacks(longPressRunnable)
                }
                if (dragging) {
                    caption.userX += event.rawX - downRawX
                    caption.userY += event.rawY - downRawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    caption.place()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> pinching = event.pointerCount > 2
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                pinching = false
                dragging = false
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    performClick()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun select(caption: TextCaption) {
        if (selected == caption) {
            caption.setSelected(true)
            caption.view.bringToFront()
            return
        }
        clearSelection(notify = false)
        selected = caption
        captions.remove(caption)
        captions.add(caption)
        caption.view.bringToFront()
        caption.setSelected(true)
        onSelectionChanged?.invoke(caption.appearance)
    }

    private fun clearSelection(notify: Boolean) {
        val current = selected ?: return
        val drop = current.editing && current.view.text.isNullOrBlank()
        current.stopEditing()
        current.setSelected(false)
        selected = null
        if (drop) {
            captions.remove(current)
            removeView(current.view)
        }
        if (notify) {
            onSelectionChanged?.invoke(null)
        }
    }

    companion object {
        const val MIN_TEXT_SP = 18f
        const val TEXT_SIZE_SPAN_SP = 46f
        private const val TEXT_WIDTH_FRACTION = 0.85f
        private const val MAX_TEXT_SP = MIN_TEXT_SP + TEXT_SIZE_SPAN_SP
    }
}

private class TextCaption(
    context: Context,
    appearance: EditorTextAppearance,
) {
    val view: EditText = EditText(context)
    var appearance: EditorTextAppearance = appearance
        private set
    var userX = 0f
    var userY = 0f
    var editing = false
    private var lift = 0f
    private var adjustingText = false
    private var trimPosted = false
    private var lastAccepted = ""
    private val horizontalPad: Int
    private val verticalPad: Int

    init {
        val resources = context.resources
        horizontalPad = resources.getDimensionPixelSize(CommonsR.dimen.medium_margin)
        verticalPad = resources.getDimensionPixelSize(CommonsR.dimen.small_margin)
        view.background = null
        view.gravity = Gravity.CENTER
        view.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
        view.setHint(R.string.enter_text)
        view.setHintTextColor(HINT_COLOR)
        view.minHeight = 0
        view.minimumHeight = 0
        view.minWidth = 0
        view.minimumWidth = 0
        view.setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad)
        view.isHorizontalScrollBarEnabled = false
        view.isSaveEnabled = false
        view.importantForAutofill = EditText.IMPORTANT_FOR_AUTOFILL_NO
        view.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        view.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        view.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        view.setTextIsSelectable(false)
        lockField()
        bindAppearance()
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> trimIfNeeded() }
    }

    fun update(next: EditorTextAppearance) {
        appearance = next
        bindAppearance()
    }

    fun bindAppearance() {
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, appearance.sizeSp)
        view.setTextColor(appearance.color)
        view.setHintTextColor(HINT_COLOR)
        view.applyEditorTextStyle(appearance.style, appearance.fontFamily)
        view.setHintTextColor(HINT_COLOR)
    }

    fun setSelected(selected: Boolean) {
        val stroke = GradientDrawable()
        val width = (view.resources.displayMetrics.density * STROKE_DP).toInt().coerceAtLeast(1)
        stroke.setColor(Color.TRANSPARENT)
        stroke.setStroke(width, SELECTION_COLOR)
        stroke.cornerRadius = view.resources.displayMetrics.density * CORNER_DP
        view.background = if (selected) stroke else null
        view.setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad)
    }

    fun contains(x: Float, y: Float): Boolean {
        val left = view.left + view.translationX
        val top = view.top + view.translationY
        val insideX = x >= left && x <= left + view.width
        val insideY = y >= top && y <= top + view.height
        return insideX && insideY
    }

    fun place() {
        view.translationX = userX
        view.translationY = userY + lift
    }

    fun startEditing() {
        if (editing) {
            return
        }
        editing = true
        view.isClickable = true
        view.isLongClickable = true
        view.movementMethod = ArrowKeyMovementMethod.getInstance()
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.isCursorVisible = true
        view.requestFocus()
        view.setSelection(view.text?.length ?: 0)
        val imm = view.context.getSystemService(InputMethodManager::class.java)
        view.post {
            if (!view.isAttachedToWindow) {
                return@post
            }
            if (!view.hasFocus()) {
                view.requestFocus()
            }
            val shown = imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT) == true
            if (shown) {
                return@post
            }
            var current: Context = view.context
            while (current is ContextWrapper && current !is Activity) {
                current = current.baseContext
            }
            val activity = current as? Activity ?: return@post
            WindowCompat.getInsetsController(activity.window, view).show(WindowInsetsCompat.Type.ime())
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun stopEditing() {
        if (!editing && lift == 0f) {
            lockField()
            return
        }
        editing = false
        lift = 0f
        place()
        view.clearFocus()
        val imm = view.context.getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        lockField()
    }

    fun updateLift(imeBottom: Int, rootHeight: Int) {
        if (!editing || imeBottom <= 0 || rootHeight <= 0) {
            lift = 0f
            place()
            return
        }
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val unliftedBottom = location[1] + view.height - lift
        val overlap = unliftedBottom - (rootHeight - imeBottom)
        val gap = view.resources.displayMetrics.density * LIFT_GAP_DP
        lift = if (overlap > 0f) -(overlap + gap) else 0f
        place()
    }

    fun toStamp(overlay: EditorTextOverlay, imageView: ImageView): EditorTextStamp? {
        val content = view.text?.toString().orEmpty()
        val displayed = imageView.displayedImageRect()
        val contentWidth = view.width - view.paddingLeft - view.paddingRight
        if (content.isBlank() || displayed == null || contentWidth <= 0) {
            return null
        }
        val imageLoc = IntArray(2)
        val overlayLoc = IntArray(2)
        imageView.getLocationInWindow(imageLoc)
        overlay.getLocationInWindow(overlayLoc)
        val contentLeft = view.left + view.translationX + view.paddingLeft
        val contentTop = view.top + view.translationY + view.paddingTop
        return EditorTextStamp(
            text = content,
            paint = TextPaint(view.paint),
            contentWidth = contentWidth,
            viewX = overlayLoc[0] - imageLoc[0] + contentLeft - displayed.left,
            viewY = overlayLoc[1] - imageLoc[1] + contentTop - displayed.top,
            displayedWidth = displayed.width(),
        )
    }

    private fun lockField() {
        view.isCursorVisible = false
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.isClickable = false
        view.isLongClickable = false
        view.movementMethod = null
    }

    private fun trimIfNeeded() {
        val current = view.text?.toString().orEmpty()
        if (adjustingText) {
            return
        }
        if (view.lineCount <= MAX_LINES) {
            lastAccepted = current
            return
        }
        if (current == lastAccepted || trimPosted) {
            return
        }
        trimPosted = true
        view.post {
            trimPosted = false
            val latest = view.text?.toString().orEmpty()
            if (latest == lastAccepted || view.lineCount <= MAX_LINES) {
                return@post
            }
            adjustingText = true
            view.setText(lastAccepted)
            view.setSelection(lastAccepted.length)
            adjustingText = false
        }
    }

    private companion object {
        const val HINT_COLOR = 0xFF9E9E9E.toInt()
        const val SELECTION_COLOR = 0xCCFFFFFF.toInt()
        const val STROKE_DP = 1f
        const val CORNER_DP = 4f
        const val LIFT_GAP_DP = 8f
        const val MAX_LINES = 6
    }
}

internal class EditorTextStamp(
    private val text: String,
    private val paint: TextPaint,
    private val contentWidth: Int,
    private val viewX: Float,
    private val viewY: Float,
    private val displayedWidth: Float,
) {
    fun drawOn(source: Bitmap): Bitmap {
        if (text.isBlank() || contentWidth <= 0 || displayedWidth <= 0f || source.width <= 0) {
            return source
        }
        val target = if (source.isMutable && source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
        }
        val scale = source.width / displayedWidth
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(true)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
        val canvas = Canvas(target)
        canvas.save()
        canvas.translate(viewX * scale, viewY * scale)
        canvas.scale(scale, scale)
        layout.draw(canvas)
        canvas.restore()
        return target
    }
}

private fun MotionEvent.fingerSpan(): Float {
    val dx = getX(0) - getX(1)
    val dy = getY(0) - getY(1)
    return hypot(dx, dy).coerceAtLeast(1f)
}

private fun ImageView.displayedImageRect(): RectF? {
    val drawable = drawable ?: return null
    val imageWidth = drawable.intrinsicWidth.toFloat()
    val imageHeight = drawable.intrinsicHeight.toFloat()
    val availableWidth = (width - paddingLeft - paddingRight).toFloat()
    val availableHeight = (height - paddingTop - paddingBottom).toFloat()
    if (imageWidth <= 0f || imageHeight <= 0f || availableWidth <= 0f || availableHeight <= 0f) {
        return null
    }
    val scale = min(availableWidth / imageWidth, availableHeight / imageHeight)
    val drawnWidth = imageWidth * scale
    val drawnHeight = imageHeight * scale
    val left = paddingLeft + (availableWidth - drawnWidth) / 2f
    val top = paddingTop + (availableHeight - drawnHeight) / 2f
    return RectF(left, top, left + drawnWidth, top + drawnHeight)
}

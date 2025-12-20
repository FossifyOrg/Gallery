package org.fossify.gallery.helpers

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import kotlin.math.abs

class VideoGestureHelper(
    private val touchSlop: Int,
    private val isPlaying: () -> Boolean,
    private val getCurrentSpeed: () -> Float,
    private val setPlaybackSpeed: (Float) -> Unit,
    private val showPill: () -> Unit,
    private val hidePill: () -> Unit,
    private val performHaptic: () -> Unit,
    private val disallowParentIntercept: () -> Unit
) {
    companion object {
        private const val TOUCH_HOLD_DURATION_MS = 500L
        private const val TOUCH_HOLD_SPEED_MULTIPLIER = 2.0f
    }

    private val handler = Handler(Looper.getMainLooper())

    private var initialX = 0f
    private var initialY = 0f
    private var originalSpeed = 1f
    internal var isLongPressActive = false

    private val touchHoldRunnable = Runnable {
        disallowParentIntercept()
        isLongPressActive = true
        originalSpeed = getCurrentSpeed()
        performHaptic()
        setPlaybackSpeed(TOUCH_HOLD_SPEED_MULTIPLIER)
        showPill()
    }

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isPlaying() && event.pointerCount == 1) {
                    initialX = event.x
                    initialY = event.y
                    handler.postDelayed(touchHoldRunnable, TOUCH_HOLD_DURATION_MS)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - initialX)
                val dy = abs(event.y - initialY)
                if (!isLongPressActive && (dx > touchSlop || dy > touchSlop)) {
                    handler.removeCallbacks(touchHoldRunnable)
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!isLongPressActive) {
                    handler.removeCallbacks(touchHoldRunnable)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(touchHoldRunnable)
                stop()
            }
        }
    }

    fun stop() {
        if (isLongPressActive) {
            setPlaybackSpeed(originalSpeed)
            isLongPressActive = false
            hidePill()
        }
    }

}

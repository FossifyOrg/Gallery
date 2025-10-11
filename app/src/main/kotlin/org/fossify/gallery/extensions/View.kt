package org.fossify.gallery.extensions

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import org.fossify.commons.extensions.toast

fun View.sendFakeClick(x: Float, y: Float) {
    val uptime = SystemClock.uptimeMillis()
    val event = MotionEvent.obtain(uptime, uptime, MotionEvent.ACTION_DOWN, x, y, 0)
    dispatchTouchEvent(event)
    event.action = MotionEvent.ACTION_UP
    dispatchTouchEvent(event)
}

fun View.showContentDescriptionOnLongClick() {
    setOnLongClickListener {
        val contentDescription = contentDescription
        if (contentDescription != null) {
            context.toast(contentDescription.toString())
        }
        true
    }
}

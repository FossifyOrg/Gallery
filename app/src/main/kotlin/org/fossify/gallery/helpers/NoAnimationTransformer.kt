package org.fossify.gallery.helpers

import android.view.View
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

class NoAnimationTransformer : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // Counteract the default sliding scroll behavior
        page.translationX = -position * page.width

        // Instantly hide off-screen pages and show the active page
        if (position <= -1.0f || position >= 1.0f) {
            page.alpha = 0.0f
            page.visibility = View.GONE
        } else if (position == 0.0f) {
            page.alpha = 1.0f
            page.visibility = View.VISIBLE
        } else {
            // Keep the fading/swapping seamless during transit
            page.alpha = 1.0f - abs(position)
            page.visibility = View.VISIBLE
        }
    }
}

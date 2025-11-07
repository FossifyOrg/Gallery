package org.fossify.gallery.activities

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.google.android.material.appbar.AppBarLayout
import org.fossify.commons.extensions.updatePaddingWithBase
import org.fossify.gallery.extensions.config

abstract class BaseViewerActivity : SimpleActivity() {
    override val padCutout: Boolean = false
    abstract val contentHolder: View
    abstract val appBarLayout: AppBarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val contentRoot = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { _, insets ->
            setupEdgeToEdge(insets)
            insets
        }
    }

    private fun setupEdgeToEdge(insets: WindowInsetsCompat) {
        if (config.showNotch) {
            val systemAndCutout =
                insets.getInsetsIgnoringVisibility(Type.systemBars() or Type.displayCutout())
            appBarLayout.updatePaddingWithBase(
                top = systemAndCutout.top,
                left = systemAndCutout.left,
                right = systemAndCutout.right
            )
        } else {
            val system = insets.getInsetsIgnoringVisibility(Type.systemBars())
            val cutout = insets.getInsetsIgnoringVisibility(Type.displayCutout())

            appBarLayout.updatePaddingWithBase(
                top = if (cutout.top > 0) 0 else system.top,
                left = if (cutout.left > 0) 0 else system.left,
                right = if (cutout.right > 0) 0 else system.right
            )

            contentHolder.updatePaddingWithBase(
                left = cutout.left,
                top = cutout.top,
                right = cutout.right,
                bottom = cutout.bottom
            )
        }
    }
}

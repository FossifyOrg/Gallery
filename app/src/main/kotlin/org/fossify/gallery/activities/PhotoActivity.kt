package org.fossify.gallery.activities

import android.os.Bundle
import org.fossify.gallery.helpers.ColorModeHelper

class PhotoActivity : PhotoVideoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mIsVideo = false
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        ColorModeHelper.resetColorMode(this)
    }
}

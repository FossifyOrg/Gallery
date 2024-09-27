package org.fossify.gallery.extensions

import androidx.media3.common.Player

fun Player.mute() {
    volume = 0f
}

fun Player.unmute() {
    volume = 1f
}

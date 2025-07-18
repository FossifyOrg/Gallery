package org.fossify.gallery.extensions

import org.fossify.commons.extensions.getFormattedDuration

fun Long.getFormattedDuration(): String {
    return (this / 1000L).toInt().getFormattedDuration()
}

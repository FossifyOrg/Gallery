package org.fossify.gallery.svg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import org.fossify.gallery.helpers.UltraHDRDecoder
import java.io.InputStream

@GlideModule
class SvgModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Register SVG support
        registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())

        // Register Ultra HDR support (prepend to handle before default decoders)
        registry.prepend(InputStream::class.java, Bitmap::class.java, UltraHDRDecoder(glide.bitmapPool))
    }

    override fun isManifestParsingEnabled() = false
}

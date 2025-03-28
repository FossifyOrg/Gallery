package org.fossify.gallery

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.caverock.androidsvg.SVG
import org.fossify.gallery.extensions.config
import org.fossify.gallery.svg.SvgDecoder
import org.fossify.gallery.svg.SvgDrawableTranscoder
import java.io.InputStream

// This class is automatically discovered by Glide thanks to the @GlideModule annotation.
// Merged functionality from original SvgModule and cache configuration.
@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Set disk cache size based on user preference
        val diskCacheSizeBytes = context.config.getThumbnailCacheSizeBytes()
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, GLIDE_CACHE_DIR, diskCacheSizeBytes))

        // Apply Fossify's preferred format
        builder.setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))

        // Configure memory cache and bitmap pool sizes
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f)
            .setBitmapPoolScreens(3f)
            .build()
        
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
        builder.setBitmapPool(LruBitmapPool(calculator.bitmapPoolSize.toLong()))
    }

    // Added from original SvgModule
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    // Disable manifest parsing to avoid potential conflicts
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    companion object {
        // Using Glide's default internal directory name allows calculating its size easily later
        const val GLIDE_CACHE_DIR = "image_manager_disk_cache"
    }
}
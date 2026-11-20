package com.liquidwidgets.app.engine

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LRU cache for fully rendered widget bitmaps.
 *
 * Full AGSL HardwareRenderer + ImageReader renders are expensive, so a widget
 * bitmap is only re-rendered when its cache key changes (size, config, palette,
 * data content, minute bucket, ripple bucket). Unchanged widgets reuse the
 * cached bitmap instead of re-running the whole GPU pipeline on every update.
 * Budget: 24 MB — oldest entries are evicted and their bitmaps recycled.
 */
object GlassRenderCache {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted) oldValue.recycle()
        }
    }

    @Synchronized
    fun getOrRender(key: String, render: () -> Bitmap): Bitmap {
        cache.get(key)?.let { return it }
        val bitmap = render()
        cache.put(key, bitmap)
        return bitmap
    }

    @Synchronized
    fun invalidateAll() = cache.evictAll()
}

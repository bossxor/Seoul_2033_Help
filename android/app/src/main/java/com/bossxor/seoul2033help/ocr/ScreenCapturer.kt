package com.bossxor.seoul2033help.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper

/**
 * Holds one VirtualDisplay for the projection's lifetime (API 34 forbids
 * a second createVirtualDisplay on the same token); grabs latest frame on demand.
 */
class ScreenCapturer(ctx: Context, private val projection: MediaProjection) {
    private val dm = ctx.resources.displayMetrics
    private val reader = ImageReader.newInstance(dm.widthPixels, dm.heightPixels, PixelFormat.RGBA_8888, 2)
    private val display: VirtualDisplay

    init {
        projection.registerCallback(object : MediaProjection.Callback() {}, Handler(Looper.getMainLooper()))
        display = projection.createVirtualDisplay(
            "ocr", dm.widthPixels, dm.heightPixels, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, null
        )
    }

    /** Latest frame or null if none produced yet. */
    fun grab(): Bitmap? {
        val img = reader.acquireLatestImage() ?: return null
        img.use {
            val p = it.planes[0]
            val rowPad = p.rowStride - p.pixelStride * it.width
            val wide = Bitmap.createBitmap(it.width + rowPad / p.pixelStride, it.height, Bitmap.Config.ARGB_8888)
            wide.copyPixelsFromBuffer(p.buffer)
            return if (rowPad == 0) wide else Bitmap.createBitmap(wide, 0, 0, it.width, it.height).also { wide.recycle() }
        }
    }

    fun release() {
        display.release()
        reader.close()
        projection.stop()
    }
}

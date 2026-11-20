package com.liquidwidgets.app.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.liquidwidgets.app.engine.MetaballSimulation
import kotlin.math.sin

/**
 * Optional live liquid-glass wallpaper.
 *
 * This is the trick that makes the "dream" look achievable on Android: a
 * normal home-screen widget cannot run a live shader, but a live wallpaper
 * CAN animate. This service draws slowly drifting liquid blobs, rising
 * bubbles and a moving specular sweep behind the widgets; the widgets sit
 * transparently on top of it (BackgroundMode TRANSPARENT / "Clear Glass"),
 * so the glass looks alive. Enable it from Settings → "Set Liquid Live
 * Wallpaper". The engine only draws while visible and stops on surface
 * destroy, so it costs nothing when the home screen is not showing.
 */
class LiquidGlassWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = LiquidEngine()

    private inner class LiquidEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var startMs = System.currentTimeMillis()
        private val frameMs = 66L // ~15 fps: smooth enough for slow liquid, cheap on battery

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, frameMs)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                startMs = System.currentTimeMillis()
                handler.removeCallbacks(drawRunnable)
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val canvas: Canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                if (w <= 0f || h <= 0f) return
                val timeSec = (System.currentTimeMillis() - startMs) / 1000f

                // Deep ocean gradient background
                val bg = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, w, h,
                        intArrayOf(Color.parseColor("#06233F"), Color.parseColor("#0B4A6B"), Color.parseColor("#06233F")),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, bg)

                // Three slowly drifting liquid blobs (metaball-style glow orbs)
                val blobs = listOf(
                    Triple(0.28f + 0.10f * sin(timeSec * 0.11f), 0.30f + 0.06f * sin(timeSec * 0.17f), Color.parseColor("#1EC8E8")),
                    Triple(0.75f + 0.08f * sin(timeSec * 0.09f + 2f), 0.62f + 0.07f * sin(timeSec * 0.13f), Color.parseColor("#8A2BE2")),
                    Triple(0.55f + 0.09f * sin(timeSec * 0.07f + 4f), 0.16f + 0.05f * sin(timeSec * 0.15f), Color.parseColor("#1FD1A5"))
                )
                for ((fx, fy, color) in blobs) {
                    val cx = w * fx
                    val cy = h * fy
                    val r = minOf(w, h) * 0.42f
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            cx, cy, r,
                            intArrayOf(Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT),
                            floatArrayOf(0f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, r, paint)
                }

                // Rising bubbles through the liquid
                MetaballSimulation.drawRisingBubbles(
                    canvas = canvas, width = w, height = h, minY = 0f,
                    timeSec = timeSec, bubbleDensity = 0.7f, speedMultiplier = 0.6f,
                    accentColor = Color.parseColor("#8DF4FF")
                )

                // Slow specular sweep across the top
                val sweepX = w * ((timeSec * 0.03f) % 1.4f - 0.2f)
                val sweep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        sweepX, 0f, sweepX + w * 0.25f, h,
                        intArrayOf(Color.TRANSPARENT, Color.argb(28, 255, 255, 255), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, sweep)
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }
}

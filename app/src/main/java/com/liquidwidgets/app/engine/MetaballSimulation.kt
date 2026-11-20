package com.liquidwidgets.app.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic, frame-rate independent fluid & bubble physics renderer for Canvas.
 * Renders rising bubbles, drifting metaball highlights, and sinusoidal liquid surface waves.
 */
object MetaballSimulation {

    fun drawLiquidFillTank(
        canvas: Canvas,
        width: Float,
        height: Float,
        fillFraction: Float,
        timeSec: Float,
        liquidColor: Int,
        highlightColor: Int,
        bubbleDensity: Float,
        viscosity: Float,
        chargingBoost: Boolean = false
    ) {
        val clampedFill = fillFraction.coerceIn(0.03f, 0.98f)
        val surfaceY = height * (1f - clampedFill)
        val waveAmp = (height * 0.032f).coerceIn(3f, 12f) * (if (chargingBoost) 1.45f else 1f)
        val speed = (if (chargingBoost) 2.4f else 1.1f) * viscosity.coerceIn(0.25f, 1.5f)

        // Back wave layer for depth
        val backPath = Path().apply {
            moveTo(0f, height)
            lineTo(0f, surfaceY)
            val steps = 28
            for (i in 0..steps) {
                val x = width * i / steps
                val phase = (i.toFloat() / steps) * (2f * PI.toFloat())
                val y = surfaceY + sin(phase * 1.4f - timeSec * speed * 1.2f + 1.2f) * waveAmp * 0.75f
                lineTo(x, y)
            }
            lineTo(width, height)
            close()
        }
        val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, Color.red(liquidColor), Color.green(liquidColor), Color.blue(liquidColor))
        }
        canvas.drawPath(backPath, backPaint)

        // Front wave layer
        val frontPath = Path().apply {
            moveTo(0f, height)
            lineTo(0f, surfaceY)
            val steps = 28
            for (i in 0..steps) {
                val x = width * i / steps
                val phase = (i.toFloat() / steps) * (2f * PI.toFloat())
                val y = surfaceY + sin(phase * 1.8f + timeSec * speed * 1.6f) * waveAmp
                lineTo(x, y)
            }
            lineTo(width, height)
            close()
        }
        val frontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(155, Color.red(liquidColor), Color.green(liquidColor), Color.blue(liquidColor))
        }
        canvas.drawPath(frontPath, frontPaint)

        // Bright meniscus crest line
        val meniscusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, Color.red(highlightColor), Color.green(highlightColor), Color.blue(highlightColor))
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
        }
        val crest = Path().apply {
            val steps = 28
            for (i in 0..steps) {
                val x = width * i / steps
                val phase = (i.toFloat() / steps) * (2f * PI.toFloat())
                val y = surfaceY + sin(phase * 1.8f + timeSec * speed * 1.6f) * waveAmp
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        canvas.drawPath(crest, meniscusPaint)

        // Rising bubbles inside the liquid column
        drawRisingBubbles(
            canvas = canvas,
            width = width,
            height = height,
            minY = surfaceY + waveAmp,
            timeSec = timeSec,
            bubbleDensity = bubbleDensity,
            speedMultiplier = if (chargingBoost) 2.5f else 1.0f,
            accentColor = highlightColor
        )
    }

    fun drawRisingBubbles(
        canvas: Canvas,
        width: Float,
        height: Float,
        minY: Float,
        timeSec: Float,
        bubbleDensity: Float,
        speedMultiplier: Float = 1f,
        accentColor: Int = Color.WHITE
    ) {
        val count = (14 * bubbleDensity.coerceIn(0f, 1.3f)).toInt()
        if (count <= 0) return

        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
        }

        val travelSpan = (height - minY).coerceAtLeast(24f)
        for (i in 0 until count) {
            val seed = (i + 1) * 1.6180339f
            val riseSpeed = (0.14f + (i % 5) * 0.045f) * speedMultiplier
            val progress = ((timeSec * riseSpeed + seed) % 1f)
            val y = height - progress * travelSpan
            if (y < minY) continue

            val baseX = ((seed * 37.3f) % 0.84f + 0.08f) * width
            val sway = sin(timeSec * 1.7f + seed * 4f) * (width * 0.025f)
            val cx = baseX + sway
            val radius = (3.5f + (i % 4) * 2.6f) * (1f - progress * 0.25f)

            bubblePaint.shader = RadialGradient(
                cx - radius * 0.3f,
                y - radius * 0.3f,
                radius * 1.2f,
                intArrayOf(
                    Color.argb(130, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                    Color.argb(35, 255, 255, 255),
                    Color.argb(165, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, y, radius, bubblePaint)
            canvas.drawCircle(cx - radius * 0.32f, y - radius * 0.32f, radius * 0.28f, specPaint)
        }
    }

    fun drawLiquidDroplet(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        tintColor: Int,
        label: String? = null,
        textColor: Int = Color.WHITE
    ) {
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx - radius * 0.35f,
                cy - radius * 0.35f,
                radius * 1.35f,
                intArrayOf(
                    Color.argb(235, 255, 255, 255),
                    Color.argb(205, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                    Color.argb(230, (Color.red(tintColor) * 0.45f).toInt(), (Color.green(tintColor) * 0.45f).toInt(), (Color.blue(tintColor) * 0.45f).toInt())
                ),
                floatArrayOf(0f, 0.42f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, radius, bodyPaint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(190, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy, radius - 1f, rimPaint)

        // Specular catchlight
        val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
        }
        canvas.drawOval(
            cx - radius * 0.45f,
            cy - radius * 0.62f,
            cx - radius * 0.05f,
            cy - radius * 0.30f,
            specPaint
        )

        if (!label.isNullOrEmpty()) {
            val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = (radius * 0.56f).coerceIn(14f, 36f)
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val yOffset = (txtPaint.descent() + txtPaint.ascent()) / 2f
            canvas.drawText(label, cx, cy - yOffset, txtPaint)
        }
    }
}

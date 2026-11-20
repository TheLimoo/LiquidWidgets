package com.liquidwidgets.app.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import com.liquidwidgets.app.data.model.LiquidShape
import com.liquidwidgets.app.data.model.WidgetType
import com.liquidwidgets.app.data.providers.QuickControlsProvider
import com.liquidwidgets.app.engine.MetaballSimulation
import com.liquidwidgets.app.receivers.WidgetActionReceiver

class LiquidQuickPillWidget : BaseLiquidGlanceWidget(WidgetType.QUICK_PILL) {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (baseConfig, palette, ripple) = loadContextState()
        val config = baseConfig.copy(shape = LiquidShape.PILL)
        val state = QuickControlsProvider.read(context)

        provideContent {
            val size = LocalSize.current
            val bmp = renderBitmap(context, size, config, palette, ripple) { canvas, w, h, txt ->
                val toggles = listOf(
                    Triple("Wi-Fi", state.wifiOn, Color.parseColor("#38E8FF")),
                    Triple("BT", state.bluetoothOn, Color.parseColor("#5C9EFF")),
                    Triple("Torch", state.torchOn, Color.parseColor("#FFD166")),
                    Triple("DND", state.dndOn, Color.parseColor("#C77DFF"))
                )

                val cy = h * 0.5f
                val r = (h * 0.34f).coerceIn(18f, 36f)
                toggles.forEachIndexed { idx, (label, isOn, activeColor) ->
                    val cx = w * (0.10f + idx * 0.145f)
                    MetaballSimulation.drawLiquidDroplet(
                        canvas = canvas,
                        cx = cx,
                        cy = cy,
                        radius = r,
                        tintColor = if (isOn) activeColor else Color.argb(120, 120, 140, 160),
                        label = label
                    )
                }

                // Glass Search Bar on the right side of the pill
                val barLeft = w * 0.64f
                val barRight = w * 0.95f
                val barRect = RectF(barLeft, h * 0.22f, barRight, h * 0.78f)
                val barFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(65, 255, 255, 255)
                }
                val barStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = Color.argb(165, 255, 255, 255)
                }
                canvas.drawRoundRect(barRect, h * 0.28f, h * 0.28f, barFill)
                canvas.drawRoundRect(barRect, h * 0.28f, h * 0.28f, barStroke)

                txt.textAlign = Paint.Align.CENTER
                txt.textSize = (h * 0.24f).coerceIn(14f, 24f)
                canvas.drawText("⌕ Search", (barLeft + barRight) * 0.5f, h * 0.58f, txt)
            }

            GlassBitmapSurface(
                bitmap = bmp,
                contentDescription = "Quick Glass Pill Controls",
                onTap = broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_SEARCH)
            ) {
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_TOGGLE_WIFI))
                    ) {}
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_TOGGLE_BT))
                    ) {}
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_TOGGLE_TORCH))
                    ) {}
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_TOGGLE_DND))
                    ) {}
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                            .clickable(broadcastAction(context, WidgetActionReceiver.ACTION_OPEN_SEARCH))
                    ) {}
                }
            }
        }
    }
}

class LiquidQuickPillWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiquidQuickPillWidget()
}

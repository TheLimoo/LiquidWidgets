# Liquid Widgets ProGuard / R8 Rules
-keep class com.liquidwidgets.app.widgets.** { *; }
-keep class com.liquidwidgets.app.receivers.** { *; }
-keep class com.liquidwidgets.app.data.providers.LiquidMediaListenerService { *; }
-dontwarn androidx.glance.**

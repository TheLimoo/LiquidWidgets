package com.liquidwidgets.app.data.providers

import android.service.notification.NotificationListenerService

/**
 * Notification-listener service so MediaSessionManager will hand us the
 * active media session. The actual session reading lives in
 * MusicDataProvider.kt — this file is only the service declaration.
 */
class LiquidMediaListenerService : NotificationListenerService()

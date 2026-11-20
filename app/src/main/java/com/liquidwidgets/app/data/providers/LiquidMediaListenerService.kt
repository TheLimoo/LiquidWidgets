package com.liquidwidgets.app.data.providers

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService

data class MusicSnapshot(
    val hasPermission: Boolean,
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val albumArt: Bitmap?,
    val packageName: String?
)

class LiquidMediaListenerService : NotificationListenerService()

object MusicDataProvider {

    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabled?.contains(context.packageName) == true
    }

    private fun getActiveController(context: Context): MediaController? {
        if (!hasNotificationAccess(context)) return null
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return null
        val component = ComponentName(context, LiquidMediaListenerService::class.java)
        val controllers = runCatching { msm.getActiveSessions(component) }.getOrNull().orEmpty()
        return controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
    }

    fun read(context: Context): MusicSnapshot {
        val hasPerm = hasNotificationAccess(context)
        val controller = getActiveController(context)
        if (!hasPerm || controller == null) {
            return MusicSnapshot(
                hasPermission = hasPerm,
                title = if (hasPerm) "Nothing Playing" else "Enable Media Access",
                artist = if (hasPerm) "Start music in any app" else "Tap to grant in Settings",
                isPlaying = false,
                albumArt = null,
                packageName = null
            )
        }

        val meta = controller.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown Track"
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: "Unknown Artist"
        val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val playing = controller.playbackState?.state == PlaybackState.STATE_PLAYING

        return MusicSnapshot(
            hasPermission = true,
            title = title,
            artist = artist,
            isPlaying = playing,
            albumArt = art,
            packageName = controller.packageName
        )
    }

    fun togglePlayPause(context: Context) {
        val controller = getActiveController(context) ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext(context: Context) {
        getActiveController(context)?.transportControls?.skipToNext()
    }

    fun openPlayerApp(context: Context) {
        val controller = getActiveController(context)
        val launch = controller?.packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (launch != null) {
            launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        }
    }
}

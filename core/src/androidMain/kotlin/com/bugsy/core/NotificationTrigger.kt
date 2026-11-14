package com.bugsy.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

private const val CHANNEL_ID = "bugsy_trigger"
private const val NOTIFICATION_ID = 0xB06
private const val ACTION_SHOW = "com.bugsy.core.SHOW_OVERLAY"

// On Android 13+ the host app must grant POST_NOTIFICATIONS; without the permission the notification simply doesn't show.
actual class NotificationTrigger private constructor(
    private val delegate: Delegate?
) {
    actual fun start() {
        delegate?.start()
    }

    actual fun stop() {
        delegate?.stop()
    }

    actual companion object {
        actual fun create(onTap: () -> Unit): NotificationTrigger {
            val context = if (BugsyContext.isHostDebuggable()) BugsyContext.applicationContext else null
            return NotificationTrigger(context?.let { Delegate(it, onTap) })
        }
    }

    private class Delegate(
        private val context: Context,
        private val onTap: () -> Unit
    ) : DefaultLifecycleObserver {

        private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onTap()
            }
        }

        fun start() {
            registerReceiver()
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }

        fun stop() {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            cancelNotification()
            runCatching { context.unregisterReceiver(receiver) }
        }

        // The notification only exists while the app is in the foreground.
        override fun onStart(owner: LifecycleOwner) {
            showNotification()
        }

        override fun onStop(owner: LifecycleOwner) {
            cancelNotification()
        }

        private fun registerReceiver() {
            val filter = IntentFilter(ACTION_SHOW)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        }

        private fun showNotification() {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Bugsy", NotificationManager.IMPORTANCE_LOW)
                )
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }
            val tap = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_SHOW).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Bugsy")
                .setContentText("Tap to open the debug panel")
                .setContentIntent(tap)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            runCatching { notificationManager.notify(NOTIFICATION_ID, notification) }
        }

        private fun cancelNotification() {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}
